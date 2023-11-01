package com.github.hokkaydo.eplbot.module.mirror.repository;

import com.github.hokkaydo.eplbot.module.mirror.model.MirrorLink;

import java.util.List;

public interface MirrorLinkRepository {

    List<MirrorLink> all();

    List<MirrorLink> readyById(Long id);

    void create(MirrorLink mirrorLink);

    void deleteByIds(Long idA, Long idB);

    boolean exists(Long idA, Long idB);

}
