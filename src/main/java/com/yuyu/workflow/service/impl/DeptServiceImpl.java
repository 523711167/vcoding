package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.OrgTypeEnum;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.UserDeptStructMapper;
import com.yuyu.workflow.convert.UserStructMapper;
import com.yuyu.workflow.eto.dept.DeptCreateETO;
import com.yuyu.workflow.eto.dept.DeptMoveETO;
import com.yuyu.workflow.eto.dept.DeptUpdateETO;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.qto.dept.DeptTreeQTO;
import com.yuyu.workflow.service.DeptService;
import com.yuyu.workflow.vo.dept.DeptTreeVO;
import com.yuyu.workflow.vo.dept.DeptVO;
import com.yuyu.workflow.vo.role.UserSimpleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.Objects;

@Service
public class DeptServiceImpl implements DeptService {

    private final UserDeptMapper userDeptMapper;
    private final UserMapper userMapper;
    private final UserDeptRelMapper userDeptRelMapper;
    private final UserDeptStructMapper userDeptStructMapper;
    private final UserStructMapper userStructMapper;

    /**
     * 注入部门模块依赖组件。
     */
    public DeptServiceImpl(UserDeptMapper userDeptMapper,
                           UserMapper userMapper,
                           UserDeptRelMapper userDeptRelMapper,
                           UserDeptStructMapper userDeptStructMapper,
                           UserStructMapper userStructMapper) {
        this.userDeptMapper = userDeptMapper;
        this.userMapper = userMapper;
        this.userDeptRelMapper = userDeptRelMapper;
        this.userDeptStructMapper = userDeptStructMapper;
        this.userStructMapper = userStructMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptVO create(DeptCreateETO eto) {
        Long parentId = Objects.isNull(eto.getParentId()) ? 0L : eto.getParentId();
        UserDept parent = getParentDept(parentId);
        validateDeptTypeAndPostType(eto.getOrgType(), eto.getPostType());
        validateParentChildRelation(parent, eto.getOrgType());
        validateLeader(eto.getLeaderId());
        assertDeptCodeUnique(parentId, eto.getCode(), null);

        UserDept entity = userDeptStructMapper.toEntity(eto);
        entity.setParentId(parentId);
        entity.setPostType(normalizePostType(eto.getOrgType(), eto.getPostType()));
        entity.setLevel(Objects.isNull(parent) ? 1 : parent.getLevel() + 1);
        // 先写入占位 path，待主键生成后再回写真实路径。
        entity.setPath("/");
        entity.setSortOrder(Objects.isNull(eto.getSortOrder()) ? 0 : eto.getSortOrder());
        entity.setLeaderName(getLeaderName(eto.getLeaderId()));
        userDeptMapper.insert(entity);

        entity.setPath(Objects.isNull(parent) ? "/" + entity.getId() + "/" : parent.getPath() + entity.getId() + "/");
        userDeptMapper.updateById(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptVO update(DeptUpdateETO eto) {
        UserDept entity = getDeptOrThrow(eto.getId());
        validateDeptTypeAndPostType(entity.getOrgType(), eto.getPostType());
        validateParentChildRelation(getParentDept(entity.getParentId()), entity.getOrgType());
        validateChildOrgTypes(entity.getId(), entity.getOrgType());
        validateLeader(eto.getLeaderId());
        assertDeptCodeUnique(entity.getParentId(), eto.getCode(), entity.getId());
        entity = userDeptStructMapper.toUpdatedEntity(eto, entity);
        entity.setPostType(normalizePostType(entity.getOrgType(), eto.getPostType()));
        entity.setSortOrder(Objects.isNull(eto.getSortOrder()) ? 0 : eto.getSortOrder());
        entity.setLeaderName(getLeaderName(eto.getLeaderId()));
        userDeptMapper.updateById(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(DeptMoveETO eto) {
        UserDept entity = getDeptOrThrow(eto.getId());
        if (entity.getId().equals(eto.getTargetParentId())) {
            throw new BizException("部门不能移动到自身下");
        }
        UserDept targetParent = getParentDept(eto.getTargetParentId());
        if (Objects.nonNull(targetParent) && targetParent.getPath().startsWith(entity.getPath())) {
            throw new BizException("目标父部门不能是当前部门或其子部门");
        }
        validateParentChildRelation(targetParent, entity.getOrgType());
        assertDeptCodeUnique(eto.getTargetParentId(), entity.getCode(), entity.getId());

        String oldPath = entity.getPath();
        int oldLevel = entity.getLevel();
        String newPath = Objects.isNull(targetParent) ? "/" + entity.getId() + "/" : targetParent.getPath() + entity.getId() + "/";
        int newLevel = Objects.isNull(targetParent) ? 1 : targetParent.getLevel() + 1;

        entity.setParentId(eto.getTargetParentId());
        entity.setPath(newPath);
        entity.setLevel(newLevel);
        userDeptMapper.updateById(entity);

        List<UserDept> subDeptList = userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                .likeRight(UserDept::getPath, oldPath)
                .orderByAsc(UserDept::getLevel));
        for (UserDept dept : subDeptList) {
            if (dept.getId().equals(entity.getId())) {
                continue;
            }
            dept.setPath(newPath + dept.getPath().substring(oldPath.length()));
            dept.setLevel(newLevel + (dept.getLevel() - oldLevel));
            userDeptMapper.updateById(dept);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> idList) {
        List<Long> deptIds = normalizeDeleteIds(idList);
        for (Long deptId : deptIds) {
            getDeptOrThrow(deptId);
        }
        long childCount = userDeptMapper.selectCount(new LambdaQueryWrapper<UserDept>()
                .in(UserDept::getParentId, deptIds));
        if (childCount > 0) {
            throw new BizException("当前部门存在子部门，无法删除");
        }
        long userCount = userDeptRelMapper.selectCount(new LambdaQueryWrapper<UserDeptRel>()
                .in(UserDeptRel::getDeptId, deptIds));
        if (userCount > 0) {
            throw new BizException("当前部门已关联用户，无法删除");
        }
        userDeptMapper.removeByIds(deptIds);
    }

    @Override
    public List<DeptTreeVO> tree(DeptTreeQTO qto) {
        List<UserDept> deptList = userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                .like(StringUtils.hasText(qto.getName()), UserDept::getName, qto.getName())
                .eq(Objects.nonNull(qto.getStatus()), UserDept::getStatus, qto.getStatus())
                .orderByAsc(UserDept::getLevel, UserDept::getSortOrder, UserDept::getId));
        if (CollectionUtils.isEmpty(deptList)) {
            return Collections.emptyList();
        }
        Map<Long, DeptTreeVO> nodeMap = new LinkedHashMap<>();
        List<DeptTreeVO> rootList = new ArrayList<>();
        for (UserDept dept : deptList) {
            DeptTreeVO node = userDeptStructMapper.toTreeVO(dept);
            node.setStatusMsg(CommonStatusEnum.getMsgById(dept.getStatus()));
            node.setOrgTypeMsg(OrgTypeEnum.getMsgByCode(dept.getOrgType()));
            nodeMap.put(node.getId(), node);
        }
        for (DeptTreeVO node : nodeMap.values()) {
            if (Objects.isNull(node.getParentId()) || node.getParentId() == 0L) {
                rootList.add(node);
                continue;
            }
            DeptTreeVO parent = nodeMap.get(node.getParentId());
            if (Objects.isNull(parent)) {
                rootList.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return rootList;
    }

    @Override
    public DeptVO detail(Long id) {
        UserDept entity = getDeptOrThrow(id);
        DeptVO vo = userDeptStructMapper.toTarget(entity);
        vo.setStatusMsg(CommonStatusEnum.getMsgById(entity.getStatus()));
        vo.setOrgTypeMsg(OrgTypeEnum.getMsgByCode(entity.getOrgType()));
        return vo;
    }

    @Override
    public List<UserSimpleVO> getUsers(Long deptId) {
        getDeptOrThrow(deptId);
        List<Long> userIds = userDeptRelMapper.selectList(new LambdaQueryWrapper<UserDeptRel>()
                        .eq(UserDeptRel::getDeptId, deptId))
                .stream()
                .map(UserDeptRel::getUserId)
                .toList();
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .map(userStructMapper::toUserSimpleVO)
                .toList();
    }

    /**
     * 按主键查询部门，不存在时抛出业务异常。
     */
    private UserDept getDeptOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        UserDept dept = userDeptMapper.selectById(id);
        if (Objects.isNull(dept)) {
            throw new BizException(RespCodeEnum.BIZ_ERROR.getId(), "部门不存在");
        }
        return dept;
    }

    /**
     * 查询父部门，顶级节点返回 null。
     */
    private UserDept getParentDept(Long parentId) {
        if (Objects.isNull(parentId) || parentId == 0L) {
            return null;
        }
        return getDeptOrThrow(parentId);
    }

    /**
     * 规范化删除主键集合，去空并去重，保证删除接口优先按批量语义执行。
     */
    private List<Long> normalizeDeleteIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            throw new BizException("idList不能为空");
        }
        List<Long> result = idList.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(result)) {
            throw new BizException("idList不能为空");
        }
        return result;
    }

    /**
     * 校验部门主管必须是有效用户。
     */
    private void validateLeader(Long leaderId) {
        if (Objects.isNull(leaderId)) {
            return;
        }
        User leader = userMapper.selectById(leaderId);
        if (Objects.isNull(leader) || !CommonStatusEnum.ENABLED.getId().equals(leader.getStatus())) {
            throw new BizException("部门主管必须是有效用户");
        }
    }

    /**
     * 根据主管用户 ID 获取主管姓名。
     */
    private String getLeaderName(Long leaderId) {
        if (Objects.isNull(leaderId)) {
            return null;
        }
        User leader = userMapper.selectById(leaderId);
        return Objects.isNull(leader) ? null : leader.getRealName();
    }

    /**
     * 校验同级部门编码唯一。
     */
    private void assertDeptCodeUnique(Long parentId, String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        UserDept exist = userDeptMapper.selectOne(new LambdaQueryWrapper<UserDept>()
                .eq(UserDept::getParentId, parentId)
                .eq(UserDept::getCode, code)
                .ne(Objects.nonNull(excludeId), UserDept::getId, excludeId)
                .last("limit 1"));
        if (Objects.nonNull(exist)) {
            throw new BizException("同级部门编码已存在");
        }
    }

    /**
     * 校验组织类型与岗位类型组合是否合法。
     */
    private void validateDeptTypeAndPostType(String orgType, String postType) {
        if (!OrgTypeEnum.containsCode(orgType)) {
            throw new BizException("orgType不合法");
        }
        if (OrgTypeEnum.POST.getCode().equals(orgType)) {
            if (!StringUtils.hasText(postType)) {
                throw new BizException("岗位类型不能为空");
            }
            return;
        }
        if (StringUtils.hasText(postType)) {
            throw new BizException("非岗位组织不能设置岗位类型");
        }
    }

    /**
     * 校验父子组织层级关系是否合法。
     */
    private void validateParentChildRelation(UserDept parent, String childOrgType) {
        if (Objects.isNull(parent)) {
            if (!OrgTypeEnum.GROUP.getCode().equals(childOrgType)) {
                throw new BizException("顶级组织只能是集团");
            }
            return;
        }
        if (OrgTypeEnum.POST.getCode().equals(parent.getOrgType())) {
            throw new BizException("岗位下不允许新增子组织");
        }
        if (OrgTypeEnum.GROUP.getCode().equals(parent.getOrgType())
                && !OrgTypeEnum.COMPANY.getCode().equals(childOrgType)) {
            throw new BizException("集团下只能新增公司");
        }
        if (OrgTypeEnum.COMPANY.getCode().equals(parent.getOrgType())
                && !OrgTypeEnum.DEPT.getCode().equals(childOrgType)) {
            throw new BizException("公司下只能新增部门");
        }
        if (OrgTypeEnum.DEPT.getCode().equals(parent.getOrgType())
                && !(OrgTypeEnum.DEPT.getCode().equals(childOrgType) || OrgTypeEnum.POST.getCode().equals(childOrgType))) {
            throw new BizException("部门下只能新增子部门或岗位");
        }
    }

    /**
     * 校验当前组织的直接子节点是否与目标组织类型兼容。
     */
    private void validateChildOrgTypes(Long deptId, String targetOrgType) {
        List<UserDept> childList = userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                .eq(UserDept::getParentId, deptId));
        for (UserDept child : childList) {
            try {
                validateParentChildRelation(buildParentForValidation(targetOrgType), child.getOrgType());
            } catch (BizException ex) {
                throw new BizException("当前组织类型与现有下级组织不匹配");
            }
        }
    }

    /**
     * 构造仅用于层级校验的父节点快照。
     */
    private UserDept buildParentForValidation(String orgType) {
        UserDept parent = new UserDept();
        parent.setOrgType(orgType);
        return parent;
    }

    /**
     * 统一归一化岗位类型字段，非岗位节点固定清空。
     */
    private String normalizePostType(String orgType, String postType) {
        if (!OrgTypeEnum.POST.getCode().equals(orgType)) {
            return null;
        }
        return postType.trim();
    }
}
