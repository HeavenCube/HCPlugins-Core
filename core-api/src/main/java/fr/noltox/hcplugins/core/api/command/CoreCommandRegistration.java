package fr.noltox.hcplugins.core.api.command;

public interface CoreCommandRegistration extends AutoCloseable {

    boolean isRegistered();

    @Override
    void close();
}
