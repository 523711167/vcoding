package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.DeptRelationTypeEnum;
import com.yuyu.workflow.common.enums.OrgTypeEnum;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserDeptRelExpand;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserDeptRelExpandMapper;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.service.UserDeptRelExpandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户组织展开关系维护服务。
 */
@Service
public class UserDeptRelExpandServiceImpl implements UserDeptRelExpandService {

    private final UserDeptRelMapper userDeptRelMapper;
    private final UserDeptRelExpandMapper userDeptRelExpandMapper;
    private final UserDeptMapper userDeptMapper;

    public UserDeptRelExpandServiceImpl(UserDeptRelMapper userDeptRelMapper,
                                        UserDeptRelExpandMapper userDeptRelExpandMapper,
                                        UserDeptMapper userDeptMapper) {
        this.userDeptRelMapper = userDeptRelMapper;
        this.userDeptRelExpandMapper = userDeptRelExpandMapper;
        this.userDeptMapper = userDeptMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildByUserIds(List<Long> userIds) {
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        if (CollectionUtils.isEmpty(normalizedUserIds)) {
            return;
        }
        deleteExpandRelations(normalizedUserIds);
        List<UserDeptRel> sourceRelations = userDeptRelMapper.selectList(new LambdaQueryWrapper<UserDeptRel>()
                .in(UserDeptRel::getUserId, normalizedUserIds)
                .orderByAsc(UserDeptRel::getUserId, UserDeptRel::getId));
        if (CollectionUtils.isEmpty(sourceRelations)) {
            return;
        }
        Map<Long, UserDept> sourceDeptMap = buildEnabledSourceDeptMap(sourceRelations);
        if (sourceDeptMap.isEmpty()) {
            return;
        }
        Map<Long, List<UserDept>> childrenMap = buildChildrenMap(sourceDeptMap.values());
        List<UserDeptRelExpand> expandRelations = new ArrayList<>();
        for (UserDeptRel relation : sourceRelations) {
            UserDept sourceDept = sourceDeptMap.get(relation.getDeptId());
            if (Objects.isNull(sourceDept)) {
                continue;
            }
            appendExpandRelations(expandRelations, sourceDept, sourceDept, relation, childrenMap,
                    DeptRelationTypeEnum.SELF.getCode(), 0);
        }
        for (UserDeptRelExpand relation : expandRelations) {
            userDeptRelExpandMapper.insert(relation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildByDeptPaths(List<String> deptPaths) {
        Set<Long> ancestorDeptIds = parseAncestorDeptIds(deptPaths);
        if (CollectionUtils.isEmpty(ancestorDeptIds)) {
            return;
        }
        List<Long> userIds = userDeptRelMapper.selectList(new LambdaQueryWrapper<UserDeptRel>()
                        .in(UserDeptRel::getDeptId, ancestorDeptIds))
                .stream()
                .map(UserDeptRel::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        rebuildByUserIds(userIds);
    }

    /**
     * 删除指定用户已有的展开关系。
     */
    private void deleteExpandRelations(List<Long> userIds) {
        List<UserDeptRelExpand> existRelations = userDeptRelExpandMapper.selectList(new LambdaQueryWrapper<UserDeptRelExpand>()
                .in(UserDeptRelExpand::getUserId, userIds));
        if (CollectionUtils.isEmpty(existRelations)) {
            return;
        }
        userDeptRelExpandMapper.removeByIds(existRelations.stream().map(UserDeptRelExpand::getId).toList());
    }

    /**
     * 仅保留当前有效且作为展开源节点可用的组织。
     */
    private Map<Long, UserDept> buildEnabledSourceDeptMap(List<UserDeptRel> sourceRelations) {
        Set<Long> sourceDeptIds = sourceRelations.stream()
                .map(UserDeptRel::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(sourceDeptIds)) {
            return Collections.emptyMap();
        }
        return userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                        .in(UserDept::getId, sourceDeptIds)
                        .eq(UserDept::getStatus, CommonStatusEnum.ENABLED.getId()))
                .stream()
                .collect(Collectors.toMap(UserDept::getId, dept -> dept, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 查询所有参与本次展开的有效节点，并按父节点归组。
     */
    private Map<Long, List<UserDept>> buildChildrenMap(Iterable<UserDept> sourceDeptList) {
        List<String> sourcePaths = new ArrayList<>();
        for (UserDept sourceDept : sourceDeptList) {
            if (StringUtils.hasText(sourceDept.getPath())) {
                sourcePaths.add(sourceDept.getPath());
            }
        }
        if (CollectionUtils.isEmpty(sourcePaths)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<UserDept> queryWrapper = new LambdaQueryWrapper<UserDept>()
                .eq(UserDept::getStatus, CommonStatusEnum.ENABLED.getId())
                .and(wrapper -> {
                    boolean first = true;
                    for (String path : sourcePaths.stream().distinct().toList()) {
                        if (!first) {
                            wrapper.or();
                        }
                        wrapper.likeRight(UserDept::getPath, path);
                        first = false;
                    }
                })
                .orderByAsc(UserDept::getLevel, UserDept::getSortOrder, UserDept::getId);
        return userDeptMapper.selectList(queryWrapper).stream()
                .collect(Collectors.groupingBy(UserDept::getParentId, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 递归写入来源节点自身及其全部有效子孙节点。
     */
    private void appendExpandRelations(List<UserDeptRelExpand> result,
                                       UserDept sourceRelationDept,
                                       UserDept currentDept,
                                       UserDeptRel directRelation,
                                       Map<Long, List<UserDept>> childrenMap,
                                       String relationType,
                                       int distance) {
        UserDeptRelExpand expandRelation = new UserDeptRelExpand();
        expandRelation.setUserId(directRelation.getUserId());
        expandRelation.setSourceRelId(directRelation.getId());
        expandRelation.setSourceDeptId(directRelation.getDeptId());
        expandRelation.setSourceOrgType(resolveSourceOrgType(directRelation, sourceRelationDept));
        expandRelation.setSourcePostType(resolveSourcePostType(directRelation, sourceRelationDept));
        expandRelation.setSourceIsPrimary(directRelation.getIsPrimary());
        expandRelation.setDeptId(currentDept.getId());
        expandRelation.setOrgType(currentDept.getOrgType());
        expandRelation.setPostType(resolveTargetPostType(currentDept));
        expandRelation.setRelationType(relationType);
        expandRelation.setDistance(distance);
        result.add(expandRelation);

        for (UserDept child : childrenMap.getOrDefault(currentDept.getId(), Collections.emptyList())) {
            appendExpandRelations(result, sourceRelationDept, child, directRelation, childrenMap,
                    DeptRelationTypeEnum.DESCENDANT.getCode(), distance + 1);
        }
    }

    /**
     * 解析来源组织类型，优先使用直接关系冗余值。
     */
    private String resolveSourceOrgType(UserDeptRel relation, UserDept sourceDept) {
        if (StringUtils.hasText(relation.getOrgType())) {
            return relation.getOrgType();
        }
        return sourceDept.getOrgType();
    }

    /**
     * 解析来源岗位类型，仅岗位来源保留岗位类型。
     */
    private String resolveSourcePostType(UserDeptRel relation, UserDept sourceDept) {
        String sourceOrgType = resolveSourceOrgType(relation, sourceDept);
        if (!OrgTypeEnum.POST.getCode().equals(sourceOrgType)) {
            return null;
        }
        if (StringUtils.hasText(relation.getPostType())) {
            return relation.getPostType();
        }
        return sourceDept.getPostType();
    }

    /**
     * 解析目标岗位类型，仅岗位节点保留岗位类型。
     */
    private String resolveTargetPostType(UserDept currentDept) {
        if (!OrgTypeEnum.POST.getCode().equals(currentDept.getOrgType())) {
            return null;
        }
        return currentDept.getPostType();
    }

    /**
     * 将变更节点路径转换为祖先节点主键集合。
     */
    private Set<Long> parseAncestorDeptIds(List<String> deptPaths) {
        if (CollectionUtils.isEmpty(deptPaths)) {
            return Collections.emptySet();
        }
        return deptPaths.stream()
                .filter(StringUtils::hasText)
                .flatMap(path -> Arrays.stream(path.split("/")))
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 规范化用户主键集合，去空并去重。
     */
    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
