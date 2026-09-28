package com.github.hokkaydo.eplbot.module.contributions.repository;

import com.github.hokkaydo.eplbot.database.CRUDRepository;
import com.github.hokkaydo.eplbot.module.contributions.model.ContributionFile;

import java.util.Set;

public interface ContributionFileRepository extends CRUDRepository<ContributionFile> {

    /**
     * @param guildId the id of the guild
     * @return the ids of all Drive files already seen for this guild
     * */
    Set<String> getFileIds(long guildId);

    /**
     * @param guildId the id of the guild
     * @param remote the rclone remote path
     * @return true if the existing files of this remote have already been recorded for this guild
     * */
    boolean isSeeded(long guildId, String remote);

    /**
     * Records that the existing files of this remote have been recorded for this guild
     * @param guildId the id of the guild
     * @param remote the rclone remote path
     * */
    void markSeeded(long guildId, String remote);

}
