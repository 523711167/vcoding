package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.BizDefinitionRoleRel;
import com.yuyu.workflow.entity.UserRole;
import com.yuyu.workflow.mapper.BizDefinitionRoleRelMapper;
import com.yuyu.workflow.mapper.BizDefinitionMapper;
import com.yuyu.workflow.mapper.UserRoleMapper;
import com.yuyu.workflow.service.BizDefinitionRoleRelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务定义角色关联服务实现。
 */
@Service
public class BizDefinitionRoleRelServiceImpl implements BizDefinitionRoleRelService {

    private final BizDefinitionRoleRelMapper bizDefinitionRoleRelMapper;
    private final BizDefinitionMapper bizDefinitionMapper;
    private final UserRoleMapper userRoleMapper;

    /**
     * 注入业务定义角色关联服务依赖。
     */
    public BizDefinitionRoleRelServiceImpl(BizDefinitionRoleRelMapper bizDefinitionRoleRelMapper,
                                           BizDefinitionMapper bizDefinitionMapper,
                                           UserRoleMapper userRoleMapper) {
        this.bizDefinitionRoleRelMapper = bizDefinitionRoleRelMapper;
        this.bizDefinitionMapper = bizDefinitionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoles(Long bizDefinitionId, List<Long> roleIds) {
        getBizDefinitionOrThrow(bizDefinitionId);
        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        validateRoleIds(normalizedRoleIds);
        removeByBizDefinitionIds(List.of(bizDefinitionId));
        for (Long roleId : normalizedRoleIds) {
            BizDefinitionRoleRel relation = new BizDefinitionRoleRel();
            relation.setBizDefinitionId(bizDefinitionId);
            relation.setRoleId(roleId);
            bizDefinitionRoleRelMapper.insert(relation);
        }
    }

    @Override
    public List<Long> listRoleIdsByBizDefinitionId(Long bizDefinitionId) {
        getBizDefinitionOrThrow(bizDefinitionId);
        return bizDefinitionRoleRelMapper.selectList(new LambdaQueryWrapper<BizDefinitionRoleRel>()
                        .eq(BizDefinitionRoleRel::getBizDefinitionId, bizDefinitionId)
                        .orderByAsc(BizDefinitionRoleRel::getId))
                .stream()
                .map(BizDefinitionRoleRel::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByBizDefinitionIds(List<Long> bizDefinitionIdList) {
        List<Long> normalizedBizDefinitionIds = normalizeBizDefinitionIds(bizDefinitionIdList);
        if (CollectionUtils.isEmpty(normalizedBizDefinitionIds)) {
            return;
        }
        List<BizDefinitionRoleRel> relations = bizDefinitionRoleRelMapper.selectAnyListByBizDefinitionIds(normalizedBizDefinitionIds);
        if (CollectionUtils.isEmpty(relations)) {
            return;
        }
        bizDefinitionRoleRelMapper.removeByIds(relations.stream()
                .map(BizDefinitionRoleRel::getId)
                .toList());
    }

    /**
     * 按业务主键查询业务定义，不存在时抛出异常。
     */
    private BizDefinition getBizDefinitionOrThrow(Long bizDefinitionId) {
        if (Objects.isNull(bizDefinitionId)) {
            throw new BizException("bizDefinitionId不能为空");
        }
        BizDefinition bizDefinition = bizDefinitionMapper.selectById(bizDefinitionId);
        if (Objects.isNull(bizDefinition)) {
            throw new BizException("业务定义不存在");
        }
        return bizDefinition;
    }

    /**
     * 校验角色主键集合必须全部存在。
     */
    private void validateRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        Set<Long> existRoleIds = userRoleMapper.selectBatchIds(roleIds).stream()
                .map(UserRole::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (existRoleIds.size() != roleIds.size()) {
            throw new BizException("存在无效角色");
        }
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

    /**
     * 规范化业务编码集合，去空并去重。
     */
    private List<Long> normalizeBizDefinitionIds(List<Long> bizDefinitionIdList) {
        if (CollectionUtils.isEmpty(bizDefinitionIdList)) {
            return Collections.emptyList();
        }
        return bizDefinitionIdList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
