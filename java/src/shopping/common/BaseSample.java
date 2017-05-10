package shopping.common;

import static shopping.common.BaseOption.CONFIG_PATH;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonError.ErrorInfo;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClient;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Base class for both sets of API samples.
 */
public abstract class BaseSample {
  protected static final String ENDPOINT_ENV_VAR = "GOOGLE_SHOPPING_SAMPLES_ENDPOINT";

  protected final Credential credential;
  protected final HttpTransport httpTransport;
  protected final Authenticator authenticator;
  protected final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
  protected final CommandLine parsedArgs;

  public BaseSample(String[] args) throws IOException {
    Options options = BaseOption.createCommandLineOptions();
    parsedArgs = parseCommandLineArguments(options, args);
    if (parsedArgs.hasOption("h")) {
      printHelpAndExit(options);
    }
    loadConfig(convertConfigPath());
    httpTransport = createHttpTransport();
    authenticator = loadAuthentication();
    credential = createCredential();
  }

  protected CommandLine parseCommandLineArguments(Options options, String[] args) {
    CommandLineParser parser = new DefaultParser();
    try {
      return parser.parse(options, args);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected void printHelpAndExit(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("samples", options, true);
    System.exit(0);
  }

  protected File convertConfigPath() throws IOException {
    String pathString = CONFIG_PATH.getOptionValue(parsedArgs);
    File path = new File(pathString);
    if (!path.exists()) {
      throw new IOException("Configuration directory '" + pathString + "' does not exist");
    }
    return path;
  }

  protected HttpTransport createHttpTransport() throws IOException {
    try {
      return GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  protected Credential createCredential() throws IOException {
    return authenticator.authenticate();
  }

  protected void checkGoogleJsonResponseException(GoogleJsonResponseException e)
      throws GoogleJsonResponseException {
    GoogleJsonError err = e.getDetails();
    // err can be null if response is not JSON
    if (err != null) {
      // For errors in the 4xx range, print out the errors and continue normally.
      if (err.getCode() >= 400 && err.getCode() < 500) {
        System.out.printf("There are %d error(s)%n", err.getErrors().size());
        for (ErrorInfo info : err.getErrors()) {
          System.out.printf("- [%s] %s%n", info.getReason(), info.getMessage());
        }
      } else {
        throw e;
      }
    } else {
      throw e;
    }
  }

  protected <T extends AbstractGoogleClient> T createService(T.Builder builder) {
    String endpoint = System.getenv(ENDPOINT_ENV_VAR);
    if (endpoint != null) {
      try {
        URI u = new URI(endpoint);
        if (!u.isAbsolute()) {
          throw new IllegalArgumentException("Endpoint URL must be absolute: " + endpoint);
        }
        builder.setRootUrl(u.resolve("/").toString());
        builder.setServicePath(u.getPath());
        System.out.println("Using non-standard API endpoint: " + endpoint);
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }
    return (T) builder.build();
  }

  protected abstract void loadConfig(File configPath) throws IOException;
  protected abstract Authenticator loadAuthentication() throws IOException;
  public abstract void execute() throws IOException;
}
