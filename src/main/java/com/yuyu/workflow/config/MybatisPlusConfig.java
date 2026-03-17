package com.yuyu.workflow.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.yuyu.workflow.common.enums.ProfileEnum;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.yuyu.workflow.mapper")
public class MybatisPlusConfig {

    private final Environment environment;

    /**
     * 注入当前运行环境，区分 dev 和 prod 配置行为。
     */
    public MybatisPlusConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * MyBatis-Plus 拦截器链。
     * 这里先注册分页插件，后续如果要加多租户、数据权限、乐观锁，也继续在这里统一挂载。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * 用 JavaBean 方式统一配置 MyBatis-Plus，而不是把策略写在 application.yml 里。
     *
     * 当前配置包含：
     * 1. 下划线转驼峰映射
     * 2. JDBC 空值处理策略
     * 3. 主键默认自增策略
     * 4. dev 环境下输出 SQL 到控制台
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPlugins(mybatisPlusInterceptor);

        com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
                new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        if (environment.acceptsProfiles(Profiles.of(ProfileEnum.DEV.getCode()))) {
            configuration.setLogImpl(StdOutImpl.class);
        }
        factoryBean.setConfiguration(configuration);

        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.AUTO);
        globalConfig.setDbConfig(dbConfig);
        factoryBean.setGlobalConfig(globalConfig);

        return factoryBean.getObject();
    }
}
