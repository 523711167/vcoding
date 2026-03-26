package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.DataScopeEnum;
import com.yuyu.workflow.common.enums.DeptRelationTypeEnum;
import com.yuyu.workflow.common.enums.OrgTypeEnum;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.entity.UserRoleDept;
import com.yuyu.workflow.entity.UserRoleDeptExpand;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserRoleDeptExpandMapper;
import com.yuyu.workflow.mapper.UserRoleDeptMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.service.UserRoleDeptExpandService;
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
 * 角色数据权限组织展开关系维护服务。
 */
@Service
public class UserRoleDeptExpandServiceImpl extends ServiceImpl<UserRoleDeptExpandMapper, UserRoleDeptExpand> implements UserRoleDeptExpandService {

    private final UserRoleDeptMapper userRoleDeptMapper;
    private final UserRoleDeptExpandMapper userRoleDeptExpandMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserDeptMapper userDeptMapper;

    public UserRoleDeptExpandServiceImpl(UserRoleDeptMapper userRoleDeptMapper,
                                         UserRoleDeptExpandMapper userRoleDeptExpandMapper,
                                         UserRoleMapper userRoleMapper,
                                         UserDeptMapper userDeptMapper) {
        this.baseMapper = userRoleDeptExpandMapper;
        this.userRoleDeptMapper = userRoleDeptMapper;
        this.userRoleDeptExpandMapper = userRoleDeptExpandMapper;
        this.userRoleMapper = userRoleMapper;
        this.userDeptMapper = userDeptMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildByRoleIds(List<Long> roleIds) {
        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        if (CollectionUtils.isEmpty(normalizedRoleIds)) {
            return;
        }
        deleteExpandRelations(normalizedRoleIds);
        Map<Long, UserRole> customScopeRoleMap = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .in(UserRole::getId, normalizedRoleIds)
                        .eq(UserRole::getDataScope, DataScopeEnum.CUSTOM_DEPT.getCode()))
                .stream()
                .collect(Collectors.toMap(UserRole::getId, role -> role, (left, right) -> left, LinkedHashMap::new));
        if (customScopeRoleMap.isEmpty()) {
            return;
        }
        List<UserRoleDept> sourceRelations = userRoleDeptMapper.selectList(new LambdaQueryWrapper<UserRoleDept>()
                .in(UserRoleDept::getRoleId, customScopeRoleMap.keySet())
                .orderByAsc(UserRoleDept::getRoleId, UserRoleDept::getId));
        if (CollectionUtils.isEmpty(sourceRelations)) {
            return;
        }
        Map<Long, UserDept> sourceDeptMap = buildEnabledSourceDeptMap(sourceRelations);
        if (sourceDeptMap.isEmpty()) {
            return;
        }
        Map<Long, List<UserDept>> childrenMap = buildChildrenMap(sourceDeptMap.values());
        List<UserRoleDeptExpand> expandRelations = new ArrayList<>();
        for (UserRoleDept relation : sourceRelations) {
            UserDept sourceDept = sourceDeptMap.get(relation.getDeptId());
            if (Objects.isNull(sourceDept)) {
                continue;
            }
            appendExpandRelations(expandRelations, sourceDept, sourceDept, relation, childrenMap,
                    DeptRelationTypeEnum.SELF.getCode(), 0);
        }
        for (UserRoleDeptExpand relation : expandRelations) {
            userRoleDeptExpandMapper.insert(relation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildByDeptPaths(List<String> deptPaths) {
        Set<Long> ancestorDeptIds = parseAncestorDeptIds(deptPaths);
        if (CollectionUtils.isEmpty(ancestorDeptIds)) {
            return;
        }
        List<Long> roleIds = userRoleDeptMapper.selectList(new LambdaQueryWrapper<UserRoleDept>()
                        .in(UserRoleDept::getDeptId, ancestorDeptIds))
                .stream()
                .map(UserRoleDept::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        rebuildByRoleIds(roleIds);
    }

    /**
     * 删除指定角色已有的展开关系。
     */
    private void deleteExpandRelations(List<Long> roleIds) {
        List<UserRoleDeptExpand> existRelations = userRoleDeptExpandMapper.selectList(new LambdaQueryWrapper<UserRoleDeptExpand>()
                .in(UserRoleDeptExpand::getRoleId, roleIds));
        if (CollectionUtils.isEmpty(existRelations)) {
            return;
        }
        userRoleDeptExpandMapper.removeByIds(existRelations.stream().map(UserRoleDeptExpand::getId).toList());
    }

    /**
     * 仅保留当前有效且可作为展开源节点的组织。
     */
    private Map<Long, UserDept> buildEnabledSourceDeptMap(List<UserRoleDept> sourceRelations) {
        Set<Long> sourceDeptIds = sourceRelations.stream()
                .map(UserRoleDept::getDeptId)
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
    private void appendExpandRelations(List<UserRoleDeptExpand> result,
                                       UserDept sourceRelationDept,
                                       UserDept currentDept,
                                       UserRoleDept directRelation,
                                       Map<Long, List<UserDept>> childrenMap,
                                       String relationType,
                                       int distance) {
        UserRoleDeptExpand expandRelation = new UserRoleDeptExpand();
        expandRelation.setRoleId(directRelation.getRoleId());
        expandRelation.setSourceRelId(directRelation.getId());
        expandRelation.setSourceDeptId(directRelation.getDeptId());
        expandRelation.setSourceOrgType(resolveSourceOrgType(directRelation, sourceRelationDept));
        expandRelation.setSourcePostType(resolveSourcePostType(directRelation, sourceRelationDept));
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
    private String resolveSourceOrgType(UserRoleDept relation, UserDept sourceDept) {
        if (StringUtils.hasText(relation.getOrgType())) {
            return relation.getOrgType();
        }
        return sourceDept.getOrgType();
    }

    /**
     * 解析来源岗位类型，仅岗位来源保留岗位类型。
     */
    private String resolveSourcePostType(UserRoleDept relation, UserDept sourceDept) {
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
     * 规范化角色主键集合，去空并去重。
     */
    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
