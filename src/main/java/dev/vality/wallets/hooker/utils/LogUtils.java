package dev.vality.wallets.hooker.utils;

import dev.vality.wallets.hooker.domain.WebHookModel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogUtils {

    public static String getLogWebHookModel(List<WebHookModel> webHookModels) {
        return webHookModels.stream()
                .map(WebHookModel::toString)
                .collect(Collectors.joining(", "));
    }
}
