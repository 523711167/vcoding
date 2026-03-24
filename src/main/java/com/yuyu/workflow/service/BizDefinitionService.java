package com.yuyu.workflow.service;

import com.yuyu.workflow.common.PageVo;
import com.yuyu.workflow.eto.biz.BizDefinitionCreateETO;
import com.yuyu.workflow.eto.biz.BizDefinitionUpdateETO;
import com.yuyu.workflow.qto.biz.BizDefinitionCurrentUserPageQTO;
import com.yuyu.workflow.qto.biz.BizDefinitionListQTO;
import com.yuyu.workflow.qto.biz.BizDefinitionPageQTO;
import com.yuyu.workflow.vo.biz.BizDefinitionCurrentUserVO;
import com.yuyu.workflow.vo.biz.BizDefinitionVO;

import java.util.List;

/**
 * 业务定义服务接口。
 */
public interface BizDefinitionService {

    /**
     * 创建业务定义。
     */
    BizDefinitionVO create(BizDefinitionCreateETO eto);

    /**
     * 修改业务定义。
     */
    BizDefinitionVO update(BizDefinitionUpdateETO eto);

    /**
     * 按主键集合批量删除业务定义。
     */
    void delete(List<Long> idList);

    /**
     * 查询业务定义列表。
     */
    List<BizDefinitionVO> list(BizDefinitionListQTO qto);

    /**
     * 分页查询业务定义列表。
     */
    PageVo<BizDefinitionVO> page(BizDefinitionPageQTO qto);

    /**
     * 分页查询当前用户可查看的业务定义。
     */
    PageVo<BizDefinitionCurrentUserVO> currentUserPage(BizDefinitionCurrentUserPageQTO qto);

    /**
     * 查询业务定义详情。
     */
    BizDefinitionVO detail(Long id);
}
