package com.yuyu.workflow.service;

import com.yuyu.workflow.eto.menu.MenuCreateETO;
import com.yuyu.workflow.eto.menu.MenuUpdateETO;
import com.yuyu.workflow.qto.menu.MenuTreeQTO;
import com.yuyu.workflow.vo.menu.MenuTreeVO;
import com.yuyu.workflow.vo.menu.MenuVO;

import java.util.List;

public interface MenuService {

    /**
     * 创建菜单。
     */
    MenuVO create(MenuCreateETO eto);

    /**
     * 更新菜单。
     */
    MenuVO update(MenuUpdateETO eto);

    /**
     * 按主键集合批量删除菜单，单个删除视为批量删除的特例。
     */
    void delete(List<Long> idList);

    /**
     * 查询菜单树。
     */
    List<MenuTreeVO> tree(MenuTreeQTO qto);

    /**
     * 查询菜单详情。
     */
    MenuVO detail(Long id);
}
