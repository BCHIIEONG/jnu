package cn.edu.jnu.labflowreport.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI labFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("实验流程与实验报告管理系统 API")
                        .description("BOOT-001 工程底座接口文档")
                        .version("v0.1.0"));
    }
}

