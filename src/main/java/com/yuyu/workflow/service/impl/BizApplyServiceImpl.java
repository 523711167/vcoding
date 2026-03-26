package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.mapper.BizApplyMapper;
import com.yuyu.workflow.service.BizApplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 业务申请服务实现。
 */
@Service
public class BizApplyServiceImpl extends ServiceImpl<BizApplyMapper, BizApply> implements BizApplyService {

    /**
     * 注入业务申请服务依赖。
     */
    public BizApplyServiceImpl(BizApplyMapper bizApplyMapper) {
        this.baseMapper = bizApplyMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(BizApply bizApply) {
        if (Objects.isNull(bizApply)) {
            throw new BizException("业务申请不能为空");
        }
        if (baseMapper.insert(bizApply) != 1) {
            throw new BizException("业务申请保存失败");
        }
        return true;
    }

    @Override
    public BizApply getByIdOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        BizApply bizApply = getById(id);
        if (Objects.isNull(bizApply)) {
            throw new BizException("业务申请不存在");
        }
        return bizApply;
    }

    @Override
    public List<BizApply> listByWorkflowInstanceIds(List<Long> workflowInstanceIdList) {
        List<Long> normalizedIds = normalizeIds(workflowInstanceIdList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<BizApply>()
                .in(BizApply::getWorkflowInstanceId, normalizedIds)
                .orderByAsc(BizApply::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(BizApply bizApply) {
        if (Objects.isNull(bizApply) || Objects.isNull(bizApply.getId())) {
            throw new BizException("业务申请id不能为空");
        }
        if (!super.updateById(bizApply)) {
            throw new BizException("业务申请更新失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> idList) {
        List<Long> normalizedIds = normalizeIds(idList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return true;
        }
        return baseMapper.removeByIds(normalizedIds) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<BizApply> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }
        for (BizApply entity : entityList) {
            save(entity);
        }
        return true;
    }

    /**
     * 规范化主键集合。
     */
    private List<Long> normalizeIds(Collection<?> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }
        return idList.stream()
                .peek(id -> Assert.isInstanceOf(Long.class, id, "主键类型必须为Long"))
                .map(Long.class::cast)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
