package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.common.enums.CommonStatusEnum;
import com.yuyu.workflow.common.enums.RespCodeEnum;
import com.yuyu.workflow.common.enums.WorkflowDefinitionStatusEnum;
import com.yuyu.workflow.common.exception.BizException;
import com.yuyu.workflow.convert.BizDefinitionStructMapper;
import com.yuyu.workflow.entity.BizDefinition;
import com.yuyu.workflow.entity.WorkflowDefinition;
import com.yuyu.workflow.eto.biz.BizDefinitionCreateETO;
import com.yuyu.workflow.eto.biz.BizDefinitionUpdateETO;
import com.yuyu.workflow.mapper.BizDefinitionMapper;
import com.yuyu.workflow.mapper.WorkflowDefinitionMapper;
import com.yuyu.workflow.qto.biz.BizDefinitionListQTO;
import com.yuyu.workflow.qto.biz.BizDefinitionPageQTO;
import com.yuyu.workflow.service.BizDefinitionRoleRelService;
import com.yuyu.workflow.service.BizDefinitionService;
import com.yuyu.workflow.vo.biz.BizDefinitionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业务定义服务实现。
 */
@Service
public class BizDefinitionServiceImpl implements BizDefinitionService {

    private final BizDefinitionMapper bizDefinitionMapper;
    private final WorkflowDefinitionMapper workflowDefinitionMapper;
    private final BizDefinitionStructMapper bizDefinitionStructMapper;
    private final BizDefinitionRoleRelService bizDefinitionRoleRelService;

    /**
     * 注入业务定义服务依赖。
     */
    public BizDefinitionServiceImpl(BizDefinitionMapper bizDefinitionMapper,
                                    WorkflowDefinitionMapper workflowDefinitionMapper,
                                    BizDefinitionStructMapper bizDefinitionStructMapper,
                                    BizDefinitionRoleRelService bizDefinitionRoleRelService) {
        this.bizDefinitionMapper = bizDefinitionMapper;
        this.workflowDefinitionMapper = workflowDefinitionMapper;
        this.bizDefinitionStructMapper = bizDefinitionStructMapper;
        this.bizDefinitionRoleRelService = bizDefinitionRoleRelService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizDefinitionVO create(BizDefinitionCreateETO eto) {
        assertBizCodeUnique(eto.getBizCode(), null);
        validateWorkflowDefinitionId(eto.getWorkflowDefinitionId());
        BizDefinition entity = bizDefinitionStructMapper.toEntity(eto);
        entity.setCreatedBy(requireCurrentUserId(eto.getCurrentUserId()));
        bizDefinitionMapper.insert(entity);
        bizDefinitionRoleRelService.replaceRoles(entity.getId(), eto.getRoleIds());
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizDefinitionVO update(BizDefinitionUpdateETO eto) {
        validateWorkflowDefinitionId(eto.getWorkflowDefinitionId());
        BizDefinition entity = bizDefinitionStructMapper.toUpdatedEntity(eto, getBizDefinitionOrThrow(eto.getId()));
        bizDefinitionMapper.updateById(entity);
        bizDefinitionRoleRelService.replaceRoles(entity.getId(), eto.getRoleIds());
        return detail(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> idList) {
        List<Long> bizDefinitionIds = normalizeDeleteIds(idList);
        bizDefinitionIds.forEach(this::getBizDefinitionOrThrow);
        bizDefinitionRoleRelService.removeByBizDefinitionIds(bizDefinitionIds);
        bizDefinitionMapper.removeByIds(bizDefinitionIds);
    }

    @Override
    public List<BizDefinitionVO> list(BizDefinitionListQTO qto) {
        return buildBizDefinitionVOList(bizDefinitionMapper.selectList(buildQuery(
                qto.getBizCode(),
                qto.getBizName(),
                qto.getWorkflowDefinitionId(),
                qto.getStatus()
        )));
    }

    @Override
    public PageVo<BizDefinitionVO> page(BizDefinitionPageQTO qto) {
        IPage<BizDefinition> page = bizDefinitionMapper.selectPage(
                new Page<>(qto.getPageNum(), qto.getPageSize()),
                buildQuery(qto.getBizCode(), qto.getBizName(), qto.getWorkflowDefinitionId(), qto.getStatus())
        );
        return PageVo.of(page.getCurrent(), page.getSize(), page.getTotal(), buildBizDefinitionVOList(page.getRecords()));
    }

    @Override
    public BizDefinitionVO detail(Long id) {
        BizDefinition entity = getBizDefinitionOrThrow(id);
        return buildBizDefinitionVO(entity, getWorkflowDefinitionMap(List.of(entity)));
    }

    /**
     * 构造业务定义查询条件。
     */
    private LambdaQueryWrapper<BizDefinition> buildQuery(String bizCode,
                                                         String bizName,
                                                         Long workflowDefinitionId,
                                                         Integer status) {
        return new LambdaQueryWrapper<BizDefinition>()
                .like(StringUtils.hasText(bizCode), BizDefinition::getBizCode, bizCode)
                .like(StringUtils.hasText(bizName), BizDefinition::getBizName, bizName)
                .eq(Objects.nonNull(workflowDefinitionId), BizDefinition::getWorkflowDefinitionId, workflowDefinitionId)
                .eq(Objects.nonNull(status), BizDefinition::getStatus, status)
                .orderByDesc(BizDefinition::getUpdatedAt, BizDefinition::getId);
    }

    /**
     * 将业务定义实体列表统一组装为返回对象列表。
     */
    private List<BizDefinitionVO> buildBizDefinitionVOList(List<BizDefinition> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return Collections.emptyList();
        }
        Map<Long, WorkflowDefinition> workflowDefinitionMap = getWorkflowDefinitionMap(entityList);
        return entityList.stream()
                .map(entity -> buildBizDefinitionVO(entity, workflowDefinitionMap))
                .toList();
    }

    /**
     * 将单个业务定义实体组装为返回对象。
     */
    private BizDefinitionVO buildBizDefinitionVO(BizDefinition entity, Map<Long, WorkflowDefinition> workflowDefinitionMap) {
        BizDefinitionVO vo = bizDefinitionStructMapper.toTarget(entity);
        vo.setStatusMsg(CommonStatusEnum.getMsgById(entity.getStatus()));
        WorkflowDefinition workflowDefinition = workflowDefinitionMap.get(entity.getWorkflowDefinitionId());
        if (Objects.nonNull(workflowDefinition)) {
            vo.setWorkflowDefinitionCode(workflowDefinition.getCode());
            vo.setWorkflowDefinitionName(workflowDefinition.getName());
        }
        return vo;
    }

    /**
     * 按主键查询业务定义，不存在时抛出业务异常。
     */
    private BizDefinition getBizDefinitionOrThrow(Long id) {
        if (Objects.isNull(id)) {
            throw new BizException("id不能为空");
        }
        BizDefinition entity = bizDefinitionMapper.selectById(id);
        if (Objects.isNull(entity)) {
            throw new BizException(RespCodeEnum.BIZ_ERROR.getId(), "业务定义不存在");
        }
        return entity;
    }

    /**
     * 校验业务编码唯一。
     */
    private void assertBizCodeUnique(String bizCode, Long excludeId) {
        BizDefinition exist = bizDefinitionMapper.selectAnyByBizCode(bizCode);
        if (Objects.nonNull(exist) && !Objects.equals(exist.getId(), excludeId)) {
            throw new BizException("业务编码已存在");
        }
    }

    /**
     * 校验绑定流程定义必须存在且已发布。
     */
    private void validateWorkflowDefinitionId(Long workflowDefinitionId) {
        WorkflowDefinition workflowDefinition = workflowDefinitionMapper.selectById(workflowDefinitionId);
        if (Objects.isNull(workflowDefinition)) {
            throw new BizException("绑定的流程定义不存在");
        }
        if (!Objects.equals(workflowDefinition.getStatus(), WorkflowDefinitionStatusEnum.PUBLISHED.getId())) {
            throw new BizException("仅允许绑定已发布流程定义");
        }
    }

    /**
     * 构造流程定义主键到实体的映射关系。
     */
    private Map<Long, WorkflowDefinition> getWorkflowDefinitionMap(List<BizDefinition> entityList) {
        Set<Long> workflowDefinitionIds = entityList.stream()
                .map(BizDefinition::getWorkflowDefinitionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollectionUtils.isEmpty(workflowDefinitionIds)) {
            return Collections.emptyMap();
        }
        return workflowDefinitionMapper.selectBatchIds(workflowDefinitionIds).stream()
                .collect(Collectors.toMap(WorkflowDefinition::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 规范化删除主键集合，去空并去重。
     */
    private List<Long> normalizeDeleteIds(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            throw new BizException("idList不能为空");
        }
        List<Long> result = idList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(result)) {
            throw new BizException("idList不能为空");
        }
        return result;
    }

    /**
     * 当前登录用户ID不能为空。
     */
    private Long requireCurrentUserId(Long currentUserId) {
        if (Objects.isNull(currentUserId)) {
            throw new BizException("当前登录用户不存在");
        }
        return currentUserId;
    }
}
