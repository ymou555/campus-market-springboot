package org.example.campusmarket.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {
    @Bean
    public DefaultKaptcha defaultKaptcha() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 验证码宽度
        properties.setProperty("kaptcha.image.width", "150");
        // 验证码高度
        properties.setProperty("kaptcha.image.height", "50");
        // 验证码字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "30");
        // 验证码字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");
        // 验证码字符集
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        // 验证码长度
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 验证码干扰线
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");
        // 验证码背景颜色
        properties.setProperty("kaptcha.background.clear.from", "255,255,255");
        properties.setProperty("kaptcha.background.clear.to", "255,255,255");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}