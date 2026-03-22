package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.OrgTypeEnum;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.enums.YesNoEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.UserDeptStructMapper;
import com.yuyu.workflow.convert.UserRoleStructMapper;
import com.yuyu.workflow.convert.UserStructMapper;
import com.yuyu.workflow.eto.user.UserCreateETO;
import com.yuyu.workflow.eto.user.UserDeptItemETO;
import com.yuyu.workflow.eto.user.UserDeptsUpdateETO;
import com.yuyu.workflow.eto.user.UserPasswordResetETO;
import com.yuyu.workflow.eto.user.UserRolesUpdateETO;
import com.yuyu.workflow.eto.user.UserStatusUpdateETO;
import com.yuyu.workflow.eto.user.UserUpdateETO;
import com.yuyu.workflow.entity.User;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.entity.UserRoleRel;
import com.yuyu.workflow.entity.UserDept;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.mapper.UserRoleRelMapper;
import com.yuyu.workflow.mapper.UserDeptMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.qto.user.UserListQTO;
import com.yuyu.workflow.qto.user.UserPageQTO;
import com.yuyu.workflow.service.UserDeptRelExpandService;
import com.yuyu.workflow.service.UserService;
import com.yuyu.workflow.vo.user.RoleSimpleVO;
import com.yuyu.workflow.vo.user.UserDeptVO;
import com.yuyu.workflow.vo.user.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final String BUILT_IN_ADMIN_USERNAME = "admin";
    private static final String BUILT_IN_ADMIN_USER_PROTECT_MESSAGE = "admin内置管理员账号不允许修改";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserDeptMapper userDeptMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final UserDeptRelMapper userDeptRelMapper;
    private final UserStructMapper userStructMapper;
    private final UserRoleStructMapper userRoleStructMapper;
    private final UserDeptStructMapper userDeptStructMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDeptRelExpandService userDeptRelExpandService;

    /**
     * 注入用户模块依赖组件。
     */
    public UserServiceImpl(UserMapper userMapper,
                           UserRoleMapper userRoleMapper,
                           UserDeptMapper userDeptMapper,
                           UserRoleRelMapper userRoleRelMapper,
                           UserDeptRelMapper userDeptRelMapper,
                           UserStructMapper userStructMapper,
                           UserRoleStructMapper userRoleStructMapper,
                           UserDeptStructMapper userDeptStructMapper,
                           PasswordEncoder passwordEncoder,
                           UserDeptRelExpandService userDeptRelExpandService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.userDeptMapper = userDeptMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.userDeptRelMapper = userDeptRelMapper;
        this.userStructMapper = userStructMapper;
        this.userRoleStructMapper = userRoleStructMapper;
        this.userDeptStructMapper = userDeptStructMapper;
        this.passwordEncoder = passwordEncoder;
        this.userDeptRelExpandService = userDeptRelExpandService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateETO eto) {
        assertUsernameUnique(eto.getUsername(), null);
        User entity = userStructMapper.toEntity(eto);
        entity.setPassword(passwordEncoder.encode(eto.getPassword()));
        entity.setStatus(CommonStatusEnum.ENABLED.getId());
        userMapper.insert(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO update(UserUpdateETO eto) {
        User entity = userStructMapper.toUpdatedEntity(eto, getWritableUserOrThrow(eto.getId()));
        userMapper.updateById(entity);
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> idList) {
        List<Long> userIds = normalizeDeleteIds(idList);
        for (Long userId : userIds) {
            User entity = getWritableUserOrThrow(userId);
            validateNoRunningTasks(entity.getId());
        }
        userMapper.removeByIds(userIds);
        deleteUserRoleRelations(userIds);
        deleteUserDeptRelations(userIds);
        userDeptRelExpandService.rebuildByUserIds(userIds);
    }

    @Override
    public List<UserVO> list(UserListQTO qto) {
        List<User> userList = userMapper.selectList(
                buildUserQuery(qto.getUsername(), qto.getRealName(), qto.getMobile(), qto.getStatus()));
        List<Long> userIds = extractUserIds(userList);
        return buildUserVOList(userList, buildUserRoleMap(userIds), buildUserDeptMap(userIds));
    }

    @Override
    public PageVo<UserVO> page(UserPageQTO qto) {
        IPage<User> page = userMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(qto.getPageNum(), qto.getPageSize()),
                buildUserQuery(qto.getUsername(), qto.getRealName(), qto.getMobile(), qto.getStatus())
        );
        List<User> userList = page.getRecords();
        List<Long> userIds = extractUserIds(userList);
        return PageVo.of(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                buildUserVOList(userList, buildUserRoleMap(userIds), buildUserDeptMap(userIds))
        );
    }

    @Override
    public UserVO detail(Long id) {
        User entity = getUserOrThrow(id);
        return userStructMapper.toUserVO(
                entity,
                buildUserRoleMap(List.of(id)).getOrDefault(id, Collections.emptyList()),
                buildUserDeptMap(List.of(id)).getOrDefault(id, Collections.emptyList())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(UserPasswordResetETO eto) {
        User entity = getWritableUserOrThrow(eto.getId());
        entity.setPassword(passwordEncoder.encode(eto.getNewPassword()));
        userMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(UserStatusUpdateETO eto) {
        getWritableUserOrThrow(eto.getId());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, eto.getId())
                .set(User::getStatus, eto.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoles(UserRolesUpdateETO eto) {
        getWritableUserOrThrow(eto.getUserId());
        List<Long> roleIds = Objects.isNull(eto.getRoleIds()) ? Collections.emptyList() : eto.getRoleIds();
        validateRoleIds(roleIds);
        deleteUserRoleRelations(List.of(eto.getUserId()));
        for (Long roleId : new LinkedHashSet<>(roleIds)) {
            UserRoleRel relation = new UserRoleRel();
            relation.setUserId(eto.getUserId());
            relation.setRoleId(roleId);
            userRoleRelMapper.insert(relation);
        }
    }

    @Override
    public List<RoleSimpleVO> getRoles(Long userId) {
        getUserOrThrow(userId);
        return buildUserRoleMap(List.of(userId)).getOrDefault(userId, Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDepts(UserDeptsUpdateETO eto) {
        getWritableUserOrThrow(eto.getUserId());
        Map<Long, UserDept> deptMap = validateDeptItems(eto.getDepts());
        deleteUserDeptRelations(List.of(eto.getUserId()));
        for (UserDeptItemETO item : eto.getDepts()) {
            UserDept dept = deptMap.get(item.getDeptId());
            UserDeptRel relation = new UserDeptRel();
            relation.setUserId(eto.getUserId());
            relation.setDeptId(item.getDeptId());
            relation.setOrgType(Objects.nonNull(dept) ? dept.getOrgType() : null);
            relation.setPostType(Objects.nonNull(dept) && OrgTypeEnum.POST.getCode().equals(dept.getOrgType())
                    ? dept.getPostType()
                    : null);
            relation.setIsPrimary(item.getIsPrimary());
            userDeptRelMapper.insert(relation);
        }
        userDeptRelExpandService.rebuildByUserIds(List.of(eto.getUserId()));
    }

    @Override
    public List<UserDeptVO> getDepts(Long userId) {
        getUserOrThrow(userId);
        return buildUserDeptMap(List.of(userId)).getOrDefault(userId, Collections.emptyList());
    }

    /**
     * 按主键查询用户，不存在时抛出业务异常。
     */
    private User getUserOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        User entity = userMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BizException(RespCodeEnum.BIZ_ERROR.getId(), "用户不存在");
        }
        return entity;
    }

    /**
     * 查询可写用户，不允许修改内置 admin 账号。
     */
    private User getWritableUserOrThrow(Long id) {
        User entity = getUserOrThrow(id);
        if (BUILT_IN_ADMIN_USERNAME.equals(entity.getUsername())) {
            throw new BizException(BUILT_IN_ADMIN_USER_PROTECT_MESSAGE);
        }
        return entity;
    }

    /**
     * 构造用户列表查询条件。
     */
    private LambdaQueryWrapper<User> buildUserQuery(String username, String realName,
                                                           String mobile, Integer status) {
        return new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(username), User::getUsername, username)
                .like(StringUtils.hasText(realName), User::getRealName, realName)
                .like(StringUtils.hasText(mobile), User::getMobile, mobile)
                .eq(Objects.nonNull(status), User::getStatus, status)
                .orderByAsc(User::getId);
    }

    /**
     * 使用已准备完成的关联数据统一组装用户返回列表，禁止在此方法内追加查询。
     */
    private List<UserVO> buildUserVOList(List<User> userList,
                                         Map<Long, List<RoleSimpleVO>> roleMap,
                                         Map<Long, List<UserDeptVO>> deptMap) {
        return userStructMapper.toUserVOList(userList, roleMap, deptMap);
    }

    /**
     * 提取用户主键集合，供批量组装角色和组织信息使用。
     */
    private List<Long> extractUserIds(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }
        return userList.stream().map(User::getId).toList();
    }

    /**
     * 校验用户名全局唯一。
     */
    private void assertUsernameUnique(String username, Long excludeId) {
        User exist = userMapper.selectAnyByUsername(username);
        if (Objects.nonNull(exist) && !exist.getId().equals(excludeId)) {
            throw new BizException("username已存在");
        }
    }

    /**
     * 删除指定用户的全部角色关联数据。
     */
    private void deleteUserRoleRelations(List<Long> userIds) {
        List<UserRoleRel> relationList = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                .in(UserRoleRel::getUserId, userIds));
        if (CollectionUtils.isEmpty(relationList)) {
            return;
        }
        userRoleRelMapper.removeByIds(relationList.stream().map(UserRoleRel::getId).toList());
    }

    /**
     * 删除指定用户的全部部门关联数据。
     */
    private void deleteUserDeptRelations(List<Long> userIds) {
        List<UserDeptRel> relationList = userDeptRelMapper.selectList(new LambdaQueryWrapper<UserDeptRel>()
                .in(UserDeptRel::getUserId, userIds));
        if (CollectionUtils.isEmpty(relationList)) {
            return;
        }
        userDeptRelMapper.removeByIds(relationList.stream().map(UserDeptRel::getId).toList());
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
     * 校验角色集合是否全部有效且启用。
     */
    private void validateRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        Set<Long> uniqueRoleIds = new LinkedHashSet<>(roleIds);
        List<UserRole> roleList = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getId, uniqueRoleIds)
                .eq(UserRole::getStatus, CommonStatusEnum.ENABLED.getId()));
        if (roleList.size() != uniqueRoleIds.size()) {
            throw new BizException("存在无效角色");
        }
    }

    /**
     * 校验用户组织关联参数，并保证唯一主组织。
     */
    private Map<Long, UserDept> validateDeptItems(List<UserDeptItemETO> deptItems) {
        if (CollectionUtils.isEmpty(deptItems)) {
            throw new BizException("用户至少需要关联一个组织");
        }
        long primaryCount = deptItems.stream()
                .filter(item -> YesNoEnum.YES.getId().equals(item.getIsPrimary()))
                .count();
        if (primaryCount != 1) {
            throw new BizException("必须且只能有一个主组织");
        }
        Set<Long> deptIds = deptItems.stream().map(UserDeptItemETO::getDeptId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (deptIds.size() != deptItems.size()) {
            throw new BizException("组织关联存在重复数据");
        }
        List<UserDept> deptList = userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                .in(UserDept::getId, deptIds)
                .eq(UserDept::getStatus, CommonStatusEnum.ENABLED.getId()));
        if (deptList.size() != deptIds.size()) {
            throw new BizException("存在无效组织");
        }
        return deptList.stream().collect(Collectors.toMap(UserDept::getId, Function.identity()));
    }

    /**
     * 构造用户到角色列表的映射关系。
     */
    private Map<Long, List<RoleSimpleVO>> buildUserRoleMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<UserRoleRel> relations = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                .in(UserRoleRel::getUserId, userIds));
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyMap();
        }
        Set<Long> roleIds = relations.stream().map(UserRoleRel::getRoleId).collect(Collectors.toSet());
        Map<Long, UserRole> roleMap = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .in(UserRole::getId, roleIds))
                .stream()
                .collect(Collectors.toMap(UserRole::getId, Function.identity()));
        Map<Long, List<RoleSimpleVO>> result = new HashMap<>();
        for (UserRoleRel relation : relations) {
            UserRole role = roleMap.get(relation.getRoleId());
            if (Objects.isNull(role)) {
                continue;
            }
            result.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>())
                    .add(userRoleStructMapper.toRoleSimpleVO(role));
        }
        return result;
    }

    /**
     * 构造用户到组织列表的映射关系。
     */
    private Map<Long, List<UserDeptVO>> buildUserDeptMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<UserDeptRel> relations = userDeptRelMapper.selectList(new LambdaQueryWrapper<UserDeptRel>()
                .in(UserDeptRel::getUserId, userIds));
        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyMap();
        }
        Set<Long> deptIds = relations.stream().map(UserDeptRel::getDeptId).collect(Collectors.toSet());
        Map<Long, UserDept> deptMap = userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>()
                        .in(UserDept::getId, deptIds))
                .stream()
                .collect(Collectors.toMap(UserDept::getId, Function.identity()));
        Map<Long, List<UserDeptVO>> result = new HashMap<>();
        for (UserDeptRel relation : relations) {
            UserDept dept = deptMap.get(relation.getDeptId());
            if (Objects.isNull(dept)) {
                continue;
            }
            UserDeptVO vo = userDeptStructMapper.toUserDeptVO(dept);
            vo.setOrgType(Objects.nonNull(relation.getOrgType()) ? relation.getOrgType() : dept.getOrgType());
            vo.setOrgTypeMsg(OrgTypeEnum.getMsgByCode(vo.getOrgType()));
            vo.setPostType(relation.getPostType());
            vo.setIsPrimary(relation.getIsPrimary());
            vo.setIsPrimaryMsg(YesNoEnum.getMsgById(relation.getIsPrimary()));
            result.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 预留进行中任务校验入口，待工作流模块接入后补充真实逻辑。
     */
    private void validateNoRunningTasks(Long userId) {
        // 待工作流模块接入后，在这里补实际的进行中任务校验。
    }
}
