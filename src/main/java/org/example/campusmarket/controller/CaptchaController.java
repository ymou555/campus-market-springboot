package org.example.campusmarket.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CaptchaController {
    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @GetMapping("/api/captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 生成验证码文本
        String text = defaultKaptcha.createText();
        // 将验证码文本保存到session
        HttpSession session = request.getSession();
        session.setAttribute("captcha", text);
        // 生成验证码图片
        BufferedImage image = defaultKaptcha.createImage(text);
        // 设置响应头
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        // 输出验证码图片
        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, "jpeg", outputStream);
        outputStream.flush();
        outputStream.close();
    }

    @PostMapping("/api/captcha/verify")
    public Map<String, Object> verifyCaptcha(@RequestParam String captcha, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String storedCaptcha = (String) session.getAttribute("captcha");
        Map<String, Object> result = new HashMap<>();
        if (storedCaptcha != null && storedCaptcha.equalsIgnoreCase(captcha)) {
            result.put("code", 200);
            result.put("message", "验证码正确");
        } else {
            result.put("code", 400);
            result.put("message", "验证码错误");
        }
        return result;
    }
}