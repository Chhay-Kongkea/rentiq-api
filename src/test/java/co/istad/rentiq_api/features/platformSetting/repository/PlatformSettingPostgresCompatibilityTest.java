package co.istad.rentiq_api.features.platformSetting.repository;

import co.istad.rentiq_api.features.platformSetting.entity.PlatformSetting;
import co.istad.rentiq_api.features.platformSetting.enums.PlatformSettingKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
class PlatformSettingPostgresCompatibilityTest {

    @Autowired PlatformSettingRepository repository;

    @Test
    void everyCatalogKeyPersistsUsingExactlyOneSupportedValueRepresentation() {
        assertThatCode(() -> {
            for (PlatformSettingKey key : PlatformSettingKey.values()) {
                PlatformSetting setting = repository.findById(key)
                        .orElseGet(() -> PlatformSetting.builder().key(key).build());
                if (key.isTextual()) {
                    setting.setValue(null);
                    setting.setTextValue(key.getDefaultTextValue());
                } else {
                    setting.setValue(key.getDefaultValue());
                    setting.setTextValue(null);
                }
                repository.saveAndFlush(setting);
            }
        }).doesNotThrowAnyException();
    }
}
