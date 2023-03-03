package org.sunbird.workflow.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.KeyData;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeyManager {
  private static Logger logger = LoggerFactory.getLogger(KeyManager.class.getName());

  private static Map<String, KeyData> keyMap = new HashMap<>();

  @Value("${accesstoken.publickey.basepath}")
  private static String path;  //not working need to change

  public static void init() {
    String basePath = path;
    if (StringUtils.isNotBlank(basePath)) {
      try (Stream<Path> walk = Files.walk(Paths.get(basePath))) {
        List<String> result =
                walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
        result.forEach(
                file -> {
                  try {
                    StringBuilder contentBuilder = new StringBuilder();
                    Path path = Paths.get(file);
                    Files.lines(path, StandardCharsets.UTF_8)
                            .forEach(
                                    x -> {
                                      contentBuilder.append(x);
                                    });
                    KeyData keyData =
                            new KeyData(
                                    path.getFileName().toString(), loadPublicKey(contentBuilder.toString()));
                    keyMap.put(path.getFileName().toString(), keyData);
                  } catch (Exception e) {
                    logger.error("KeyManager:init: exception in reading public keys ", e);
                  }
                });
      } catch (Exception e) {
        logger.error("KeyManager:init: exception in loading publickeys ", e);
      }
    }
  }

  public static KeyData getPublicKey(String keyId) {
    init();
    return keyMap.get(keyId);
  }

  public static PublicKey loadPublicKey(String key) throws Exception {
    String publicKey = new String(key.getBytes(), StandardCharsets.UTF_8);
    publicKey = publicKey.replaceAll("(-+BEGIN PUBLIC KEY-+)", "");
    publicKey = publicKey.replaceAll("(-+END PUBLIC KEY-+)", "");
    publicKey = publicKey.replaceAll("[\\r\\n]+", "");
    byte[] keyBytes = Base64Util.decode(publicKey.getBytes("UTF-8"), Base64Util.DEFAULT);

    X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(keyBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePublic(X509publicKey);
  }
}