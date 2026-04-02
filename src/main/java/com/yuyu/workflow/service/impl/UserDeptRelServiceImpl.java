package com.yuyu.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuyu.workflow.entity.UserDeptRel;
import com.yuyu.workflow.mapper.UserDeptRelMapper;
import com.yuyu.workflow.service.UserDeptRelService;
import org.springframework.stereotype.Service;

/**
 * 用户组织直接关联服务实现。
 */
@Service
public class UserDeptRelServiceImpl extends ServiceImpl<UserDeptRelMapper, UserDeptRel> implements UserDeptRelService {
}

