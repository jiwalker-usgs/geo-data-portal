package gov.usgs.cida.gdp.urs;

import gov.usgs.cida.gdp.constants.AppConstant;
import gov.usgs.cida.proxy.registration.HttpLoginProvider;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jordan Walker <jiwalker@usgs.gov>
 */
public class URSLoginProvider implements HttpLoginProvider {

	private static final Logger log = LoggerFactory.getLogger(URSLoginProvider.class);

	private static final int SSL_PORT = 443;

	private String ursHost;
	private ClientConnectionManager ccm;
	private CredentialsProvider credentialStore;
	private CookieStore cookieStore;
	
	private boolean ursProtected = false;
	private boolean loggedIn = false;

	public URSLoginProvider() {
		this(AppConstant.URS_USERNAME.getValue(),
				AppConstant.URS_PASSWORD.getValue(),
				AppConstant.URS_HOST.getValue());
	}

	public URSLoginProvider(String username, String password, String ursHost) {
		this.ursHost = ursHost;
		
		this.credentialStore = new BasicCredentialsProvider();
		this.credentialStore.setCredentials(new AuthScope(ursHost, SSL_PORT),
				new UsernamePasswordCredentials(username, password));
		this.cookieStore = new BasicCookieStore();
		initClientConnectionManager();
		// TODO add trust store to avoid trusting everything
	}

	@Override
	public boolean checkResource(URL resource) {
		try {
			String currentResource = resource.toString();
			int statusCode = 302;
			int redirects = 0;
			while (statusCode == 302 && redirects < 10) {
				redirects++;
				HttpParams params = new BasicHttpParams();
				params.setParameter("http.protocol.handle-redirects", false);
				HttpClient client = getClient(params);
				HttpGet get = new HttpGet(currentResource);
				
				HttpResponse response = null;
				try {
					response = client.execute(get);
				
					statusCode = response.getStatusLine().getStatusCode();

					if (statusCode == 302) {
						Header location = response.getLastHeader("Location");
						if (location != null) {
							URL url = new URL(location.getValue());
							if (ursHost.equals(url.getHost())) {
								ursProtected = true;
							}
							currentResource = location.getValue();
						} else {
							throw new IllegalStateException("Received redirect without Location header");
						}
					} else if (statusCode >= 400) {
						throw new RuntimeException(response.getStatusLine().getReasonPhrase());
					}
				} finally {
					HttpClientUtils.closeQuietly(response);
				}
			}

			if (statusCode == 200) {
				loggedIn = true;
			}
		} catch (IOException ex) {
			log.error("Error getting resource", ex);
		}
		return ursProtected && loggedIn;
	}

	@Override
	public HttpClient getClient(HttpParams params) {
		DefaultHttpClient client = new DefaultHttpClient(ccm, params);
		client.setCookieStore(cookieStore);
		client.setCredentialsProvider(credentialStore);
		return client;
	}
	
	@Override
	public void close() {
		cookieStore.clear();
		credentialStore.clear();
		if (ccm != null) {
			ccm.shutdown();
		}
	}
	
	private void initClientConnectionManager() {
		SSLSocketFactory socketFactory = null;
		try {
			socketFactory = new SSLSocketFactory((X509Certificate[] xcs, String string) ->
					true, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
			throw new RuntimeException("Couldn't create socket factory", ex);
		}
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", 80, new PlainSocketFactory()));
		registry.register(new Scheme("https", 443, socketFactory));
		ccm = new PoolingClientConnectionManager(registry);
	}

}
