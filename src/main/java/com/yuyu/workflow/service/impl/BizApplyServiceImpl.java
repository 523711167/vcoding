package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.mapper.BizApplyMapper;
import com.yuyu.workflow.service.BizApplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 业务申请服务实现。
 */
@Service
public class BizApplyServiceImpl implements BizApplyService {

    private final BizApplyMapper bizApplyMapper;

    /**
     * 注入业务申请服务依赖。
     */
    public BizApplyServiceImpl(BizApplyMapper bizApplyMapper) {
        this.bizApplyMapper = bizApplyMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(BizApply bizApply) {
        if (Objects.isNull(bizApply)) {
            throw new BizException("业务申请不能为空");
        }
        if (bizApplyMapper.insert(bizApply) != 1) {
            throw new BizException("业务申请保存失败");
        }
    }

    @Override
    public BizApply getByIdOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        BizApply bizApply = bizApplyMapper.selectById(id);
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
        return bizApplyMapper.selectList(new LambdaQueryWrapper<BizApply>()
                .in(BizApply::getWorkflowInstanceId, normalizedIds)
                .orderByAsc(BizApply::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(BizApply bizApply) {
        if (Objects.isNull(bizApply) || Objects.isNull(bizApply.getId())) {
            throw new BizException("业务申请id不能为空");
        }
        if (bizApplyMapper.updateById(bizApply) != 1) {
            throw new BizException("业务申请更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByIds(List<Long> idList) {
        List<Long> normalizedIds = normalizeIds(idList);
        if (CollectionUtils.isEmpty(normalizedIds)) {
            return;
        }
        bizApplyMapper.removeByIds(normalizedIds);
    }

    /**
     * 规范化主键集合。
     */
    private List<Long> normalizeIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Collections.emptyList();
        }
        return idList.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
