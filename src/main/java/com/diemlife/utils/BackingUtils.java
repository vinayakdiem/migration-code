package com.diemlife.utils;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import com.diemlife.models.FundraisingLink;
import com.diemlife.models.Quests;

import lombok.NoArgsConstructor;
import play.Logger;

@NoArgsConstructor(access = PRIVATE)
public abstract class BackingUtils {

    public static Integer getSelectedMultiSellerId(final Quests quest, final FundraisingLink link, final Integer brandUserId) {
        if (quest == null) {
            Logger.warn("Multi-seller lookup failed for null Quest");

            return null;
        }
        if (quest.isMultiSellerEnabled()) {
            if (brandUserId == null) {
                if (quest.isFundraising()) {
                    if (link == null || link.brand == null) {
                        Logger.warn("Multi-seller lookup failed for null fundraising link of Quest with ID " + quest.getId());

                        return null;
                    } else {
                        Logger.info(format("Multi-seller lookup returned user ID %s for fundraising link of Quest with ID %s", link.brand.getUserId(), quest.getId()));

                        return link.brand.getUserId();
                    }
                } else {
                    Logger.warn("Multi-seller lookup failed for non-fundraising Quest with ID " + quest.getId());

                    return null;
                }
            } else {
                Logger.info(format("Multi-seller lookup returned user ID %s as form value provided for Quest with ID %s", brandUserId, quest.getId()));

                return brandUserId;
            }
        } else {
            Logger.debug("Multi-seller feature is not enabled for Quest with ID " + quest.getId());

            return null;
        }
    }

}
