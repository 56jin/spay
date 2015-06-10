package services.goldway.api;

import models.TradeResult;

/**
 * Created by Yuan on 2015/6/10.
 */
public class FeedbackResultFactory {

    private FeedbackResultFactory() {
    }

    public static Class<?> createFeedbackResult(String serviceCode){
        return Enum.valueOf(FeedbackType.class, serviceCode).getClazz();
    }

    public enum FeedbackType {

        NCPS1002(TradeResult.class);

        private Class<?> clazz;

        FeedbackType(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Class<?> getClazz() {
            return clazz;
        }

    }
}
