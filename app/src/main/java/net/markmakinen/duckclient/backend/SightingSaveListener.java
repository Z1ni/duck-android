package net.markmakinen.duckclient.backend;

/**
 * Created by Zini on 17.12.2016 16.12.
 */

public interface SightingSaveListener {
    void saveCompleted();
    void saveFailed(String msg);
}
