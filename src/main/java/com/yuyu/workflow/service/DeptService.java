package com.yuyu.workflow.service;

import com.yuyu.workflow.eto.dept.DeptCreateETO;
import com.yuyu.workflow.eto.dept.DeptMoveETO;
import com.yuyu.workflow.eto.dept.DeptUpdateETO;
import com.yuyu.workflow.qto.dept.DeptTreeQTO;
import com.yuyu.workflow.vo.dept.DeptTreeVO;
import com.yuyu.workflow.vo.dept.DeptVO;
import com.yuyu.workflow.vo.role.UserSimpleVO;

import java.util.List;

public interface DeptService {

    /**
     * 创建部门。
     */
    DeptVO create(DeptCreateETO eto);

    /**
     * 更新部门信息。
     */
    DeptVO update(DeptUpdateETO eto);

    /**
     * 移动部门。
     */
    void move(DeptMoveETO eto);

    /**
     * 按主键集合批量删除部门，单个删除视为批量删除的特例。
     */
    void delete(List<Long> idList);

    /**
     * 查询部门树。
     */
    List<DeptTreeVO> tree(DeptTreeQTO qto);

    /**
     * 查询部门详情。
     */
    DeptVO detail(Long id);

    /**
     * 查询部门下用户。
     */
    List<UserSimpleVO> getUsers(Long deptId);
}
