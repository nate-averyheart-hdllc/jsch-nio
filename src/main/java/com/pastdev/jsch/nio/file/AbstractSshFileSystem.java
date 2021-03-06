package com.pastdev.jsch.nio.file;


import static com.pastdev.jsch.nio.file.UnixSshFileSystemProvider.PATH_SEPARATOR;


import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Proxy;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.SessionFactory.SessionFactoryBuilder;
import com.pastdev.jsch.command.CommandRunner;


public abstract class AbstractSshFileSystem extends FileSystem {
    private static final Set<String> supportedFileAttributeViews;

    static {
        supportedFileAttributeViews = new HashSet<String>();
        supportedFileAttributeViews.add( "basic" );
    }

    private String binDir = null;
    private CommandRunner commandRunner;
    private Map<String, ?> environment;
    private AbstractSshFileSystemProvider provider;
    private URI uri;

    public AbstractSshFileSystem( AbstractSshFileSystemProvider provider, URI uri, Map<String, ?> environment ) throws IOException {
        this.provider = provider;
        this.uri = uri;
        this.environment = environment;

        String binDirKey = "dir.bin";
        if ( environment.containsKey( binDirKey ) ) {
            binDir = (String)environment.get( binDirKey );
        }

        try {
            // Construct a new sessionFactory from the URI authority, path, and
            // optional environment proxy
            SessionFactory defaultSessionFactory = (SessionFactory)environment.get( "defaultSessionFactory" );
            if ( defaultSessionFactory == null ) {
                throw new IllegalArgumentException( "defaultSessionFactory environment parameter is required" );
            }
            SessionFactoryBuilder builder = defaultSessionFactory.newSessionFactoryBuilder();
            String username = uri.getUserInfo();
            if ( username != null ) {
                builder.setUsername( username );
            }
            String hostname = uri.getHost();
            if ( hostname != null ) {
                builder.setHostname( hostname );
            }
            int port = uri.getPort();
            if ( port != -1 ) {
                builder.setPort( port );
            }
            Proxy proxy = (Proxy)environment.get( "proxy" );
            if ( proxy != null ) {
                builder.setProxy( proxy );
            }
            this.commandRunner = new CommandRunner( builder.build() );
        }
        catch ( JSchException e ) {
            throw new IOException( e );
        }
    }

    String getCommand( String command ) {
        String commandKey = "command." + command;
        if ( environment.containsKey( commandKey ) ) {
            return (String)environment.get( commandKey );
        }
        else if ( binDir != null ) {
            return binDir + PATH_SEPARATOR + command;
        }
        else {
            return command;
        }
    }

    public CommandRunner getCommandRunner() {
        return commandRunner;
    }

    public Object getFromEnvironment( String key ) {
        return environment.get( key );
    }

    public Long getLongFromEnvironment( String key ) {
        Object value = environment.get( key );
        if ( value == null ) {
            return null;
        }
        if ( value instanceof Long ) {
            return (long)value;
        }
        return Long.parseLong( value.toString() );
    }
    
    public String getStringFromEnvironment( String key ) {
        Object value = environment.get( key );
        if ( value == null ) {
            return null;
        }
        if ( value instanceof String ) {
            return (String)value;
        }
        return value.toString();
    }
    
    public TimeUnit getTimeUnitFromEnvironment( String key ) {
        Object value = environment.get( key );
        if ( value == null ) {
            return null;
        }
        if ( value instanceof TimeUnit ) {
            return (TimeUnit)value;
        }
        return TimeUnit.valueOf( value.toString().toUpperCase() );
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return supportedFileAttributeViews;
    }
}
