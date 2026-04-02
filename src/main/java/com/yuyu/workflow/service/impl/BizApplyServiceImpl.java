package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.BizApplyStatusEnum;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.struct.BizApplyStructMapper;
import com.yuyu.workflow.entity.BizApply;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.eto.biz.BizApplySaveDraftETO;
import com.yuyu.workflow.eto.biz.BizApplyUpdateDraftETO;
import com.yuyu.workflow.mapper.BizApplyMapper;
import com.yuyu.workflow.mapper.UserMapper;
import com.yuyu.workflow.qto.biz.BizApplyDraftIdQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftListQTO;
import com.yuyu.workflow.qto.biz.BizApplyDraftPageQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryDetailQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryListQTO;
import com.yuyu.workflow.qto.workflow.WorkflowQueryPageQTO;
import com.yuyu.workflow.service.BizApplyService;
import com.yuyu.workflow.service.BizDefinitionService;
import com.yuyu.workflow.service.WorkflowDefinitionService;
import com.yuyu.workflow.vo.biz.BizApplyDraftVO;
import com.yuyu.workflow.vo.workflow.WorkflowQueryVO;
import org.apache.commons.lang3.StringUtils;
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

    private final UserMapper userMapper;
    private final BizDefinitionService bizDefinitionService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final BizApplyStructMapper bizApplyStructMapper;

    /**
     * 注入业务申请服务依赖。
     */
    public BizApplyServiceImpl(BizApplyMapper bizApplyMapper,
                               UserMapper userMapper,
                               BizDefinitionService bizDefinitionService,
                               WorkflowDefinitionService workflowDefinitionService,
                               BizApplyStructMapper bizApplyStructMapper) {
        this.baseMapper = bizApplyMapper;
        this.userMapper = userMapper;
        this.bizDefinitionService = bizDefinitionService;
        this.workflowDefinitionService = workflowDefinitionService;
        this.bizApplyStructMapper = bizApplyStructMapper;
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
    @Transactional(rollbackFor = Exception.class)
    public BizApply saveDraft(BizApplySaveDraftETO eto) {
        BizDefinition bizDefinition = getBizDefinitionOrThrow(eto.getBizDefinitionId());

        BizApply bizApply = new BizApply();
        bizApply.setBizDefinitionId(bizDefinition.getId());
        bizApply.setBizName(bizDefinition.getBizName());
        bizApply.setTitle(eto.getTitle());
        bizApply.setBizStatus(BizApplyStatusEnum.DRAFT.getCode());
        bizApply.setApplicantId(eto.getCurrentUserId());
        bizApply.setApplicantName(eto.getCurrentUsername());
        bizApply.setDeptId(eto.getCurrentPrimaryDeptId());
        bizApply.setFormData(eto.getFormData());
        bizApply.setWorkflowName(resolveWorkflowName(bizDefinition));
        save(bizApply);
        return bizApply;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizApply updateDraft(BizApplyUpdateDraftETO eto) {
        BizApply current = getByIdOrThrow(eto.getId());
        assertDraftOwner(current, eto.getCurrentUserId());
        assertDraftEditable(current);

        BizDefinition bizDefinition = getBizDefinitionOrThrow(eto.getBizDefinitionId());
        BizApply update = new BizApply();
        update.setId(current.getId());
        update.setBizDefinitionId(bizDefinition.getId());
        update.setBizName(bizDefinition.getBizName());
        update.setTitle(eto.getTitle());
        update.setDeptId(eto.getCurrentPrimaryDeptId());
        update.setFormData(eto.getFormData());
        update.setWorkflowName(resolveWorkflowName(bizDefinition));
        updateById(update);
        return getByIdOrThrow(current.getId());
    }

    @Override
    public void submitCheck(BizApply bizApply, Long currentUserId) {
        if (Objects.isNull(bizApply)) {
            throw new BizException("业务申请不存在");
        }
        assertDraftOwner(bizApply, currentUserId);
        if (!BizApplyStatusEnum.isDraft(bizApply.getBizStatus())) {
            throw new BizException("当前业务申请不是草稿状态，不能提交");
        }
        if (Objects.nonNull(bizApply.getWorkflowInstanceId())) {
            throw new BizException("当前业务申请已发起流程，不能重复提交");
        }
        BizDefinition bizDefinition = getBizDefinitionOrThrow(bizApply.getBizDefinitionId());
        if (StringUtils.isBlank(bizDefinition.getWorkflowDefinitionCode())) {
            throw new BizException("业务定义未绑定流程");
        }
        WorkflowDefinition workflowDefinition = workflowDefinitionService.getLatestPublishedByCode(bizDefinition.getWorkflowDefinitionCode());
        if (Objects.isNull(workflowDefinition)) {
            throw new BizException("绑定的流程未发布");
        }
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
    public List<BizApplyDraftVO> listDrafts(BizApplyDraftListQTO qto) {
        return baseMapper.selectList(buildDraftQuery(qto.getCurrentUserId(), qto.getBizDefinitionId(), qto.getTitle()))
                .stream()
                .map(bizApplyStructMapper::toBizApplyDraftVO)
                .toList();
    }

    @Override
    public BizApplyDraftVO detailDraft(BizApplyDraftIdQTO qto) {
        BizApply bizApply = getByIdOrThrow(qto.getId());
        assertDraftOwner(bizApply, qto.getCurrentUserId());
        if (!BizApplyStatusEnum.isDraft(bizApply.getBizStatus())) {
            throw new BizException("当前业务申请不是草稿状态");
        }
        return bizApplyStructMapper.toBizApplyDraftVO(bizApply);
    }

    @Override
    public PageVo<BizApplyDraftVO> pageDrafts(BizApplyDraftPageQTO qto) {
        IPage<BizApply> page = baseMapper.selectPage(
                new Page<>(qto.getPageNum(), qto.getPageSize()),
                buildDraftQuery(qto.getCurrentUserId(), qto.getBizDefinitionId(), qto.getTitle())
        );
        return PageVo.of(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getRecords().stream()
                        .map(bizApplyStructMapper::toBizApplyDraftVO)
                .toList()
        );
    }

    @Override
    public List<WorkflowQueryVO> listQueries(WorkflowQueryListQTO qto) {
        return baseMapper.selectQueryList(qto);
    }

    @Override
    public IPage<WorkflowQueryVO> pageQueries(IPage<WorkflowQueryVO> page, WorkflowQueryPageQTO qto) {
        return baseMapper.selectQueryPage(page, qto);
    }

    @Override
    public WorkflowQueryVO detailQuery(WorkflowQueryDetailQTO qto) {
        return baseMapper.selectQueryDetail(qto);
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

    /**
     * 查询业务定义并校验启用状态。
     */
    private BizDefinition getBizDefinitionOrThrow(Long bizDefinitionId) {
        if (Objects.isNull(bizDefinitionId)) {
            throw new BizException("bizDefinitionId不能为空");
        }
        BizDefinition bizDefinition = bizDefinitionService.getById(bizDefinitionId);
        if (Objects.isNull(bizDefinition)) {
            throw new BizException("业务定义不存在");
        }
        if (!Objects.equals(bizDefinition.getStatus(), CommonStatusEnum.ENABLED.getId())) {
            throw new BizException("业务定义已停用");
        }
        return bizDefinition;
    }


    /**
     * 校验草稿归属人。
     */
    private void assertDraftOwner(BizApply bizApply, Long currentUserId) {
        if (!Objects.equals(bizApply.getApplicantId(), currentUserId)) {
            throw new BizException("无权操作该业务申请");
        }
    }

    /**
     * 校验草稿仍可编辑。
     */
    private void assertDraftEditable(BizApply bizApply) {
        if (!BizApplyStatusEnum.isDraft(bizApply.getBizStatus())) {
            throw new BizException("当前业务申请不是草稿状态，不能修改");
        }
        if (Objects.nonNull(bizApply.getWorkflowInstanceId())) {
            throw new BizException("当前业务申请已发起流程，不能修改");
        }
    }

    /**
     * 解析当前业务绑定的流程名称快照。
     */
    private String resolveWorkflowName(BizDefinition bizDefinition) {
        if (StringUtils.isBlank(bizDefinition.getWorkflowDefinitionCode())) {
            return null;
        }
        WorkflowDefinition workflowDefinition = workflowDefinitionService.getLatestPublishedByCode(bizDefinition.getWorkflowDefinitionCode());
        return Objects.nonNull(workflowDefinition) ? workflowDefinition.getName() : null;
    }

    /**
     * 构造当前用户草稿箱查询条件。
     */
    private LambdaQueryWrapper<BizApply> buildDraftQuery(Long currentUserId, Long bizDefinitionId, String title) {
        if (Objects.isNull(currentUserId)) {
            throw new BizException("当前用户不能为空");
        }
        return new LambdaQueryWrapper<BizApply>()
                .eq(BizApply::getApplicantId, currentUserId)
                .eq(BizApply::getBizStatus, BizApplyStatusEnum.DRAFT.getCode())
                .eq(Objects.nonNull(bizDefinitionId), BizApply::getBizDefinitionId, bizDefinitionId)
                .like(StringUtils.isNotBlank(title), BizApply::getTitle, title)
                .orderByDesc(BizApply::getUpdatedAt, BizApply::getId);
    }
}
