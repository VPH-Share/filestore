/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.uva.cs.lobcder.frontend;

import io.milton.common.Path;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.servlet.MiltonFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.java.Log;
import nl.uva.cs.lobcder.catalogue.JDBCatalogue;
import nl.uva.cs.lobcder.optimization.FileAccessPredictor;
import nl.uva.cs.lobcder.optimization.LobState;
import nl.uva.cs.lobcder.optimization.MyTask;
import nl.uva.cs.lobcder.util.CatalogueHelper;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author S. Koulouzis
 */
@Log
public class MyFilter extends MiltonFilter {

    private JDBCatalogue catalogue;
    private static FileAccessPredictor fap;
    private static LobState prevState;
    private static final BlockingQueue queue = new ArrayBlockingQueue(2500);
    private static RequestEventRecorder recorder;
    private Timer recordertimer;

    public MyFilter() throws Exception {
//        getFileAccessPredictor();
    }

    @Override
    public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp, javax.servlet.FilterChain fc) throws IOException, ServletException {
        double start = System.currentTimeMillis();
        String method = ((HttpServletRequest) req).getMethod();
        StringBuffer reqURL = ((HttpServletRequest) req).getRequestURL();



//        long startPredict = System.currentTimeMillis();
//        predict(Request.Method.valueOf(method), reqURL.toString());
//        long elapsedPredict = System.currentTimeMillis() - startPredict;
//        log.log(Level.INFO, "elapsedPredict: {0}", elapsedPredict);

        super.doFilter(req, resp, fc);
        double elapsed = System.currentTimeMillis() - start;

        String userAgent = ((HttpServletRequest) req).getHeader("User-Agent");

        String from = ((HttpServletRequest) req).getRemoteAddr();
//        String user = ((HttpServletRequest) req).getRemoteUser();
        int contentLen = ((HttpServletRequest) req).getContentLength();
        String contentType = ((HttpServletRequest) req).getContentType();

        String authorizationHeader = ((HttpServletRequest) req).getHeader("authorization");
        String userNpasswd = "";
        if (authorizationHeader != null) {
            userNpasswd = authorizationHeader.split("Basic ")[1];
        }

        RequestWapper my = new RequestWapper();
        my.setMethod(method);
        my.setContentLength(contentLen);
        my.setContentType(contentType);
        my.setTimeStamp(System.currentTimeMillis());
        my.setElapsed(elapsed);
        my.setRemoteAddr(from);
        my.setRequestURL(((HttpServletRequest) req).getRequestURL().toString());
        my.setUserAgent(userAgent);
        my.setUserNpasswd(getUserName((HttpServletRequest) req));
        queue.add(my);
        startRecorder();

        log.log(Level.INFO, "Req_Source: {0} Method: {1} Content_Len: {2} Content_Type: {3} Elapsed_Time: {4} sec EncodedUser: {5} UserAgent: {6}", new Object[]{from, method, contentLen, contentType, elapsed / 1000.0, userNpasswd, userAgent});
//        try (Connection connection = getCatalogue().getConnection()) {
//            recordEvent(connection, ((HttpServletRequest) req), elapsed);
//        } catch (SQLException ex) {
//            Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    private String getUserName(HttpServletRequest httpServletRequest) throws UnsupportedEncodingException {
        String authorizationHeader = httpServletRequest.getHeader("authorization");
        String userNpasswd = "";
        if (authorizationHeader != null) {
            final int index = authorizationHeader.indexOf(' ');
            if (index > 0) {
                final String credentials = new String(Base64.decodeBase64(authorizationHeader.substring(index).getBytes()), "UTF8");
                String[] encodedToken = credentials.split(":");
                if (encodedToken.length > 1) {
                    String token = new String(Base64.decodeBase64(encodedToken[1]));
                    if (token.contains(";") && token.contains("uid=")) {
                        String uid = token.split(";")[0];
                        userNpasswd = uid.split("uid=")[1];
                    } else {
                        userNpasswd = credentials.substring(0, credentials.indexOf(":"));
                    }
                }
//                    if (userNpasswd == null || userNpasswd.length() < 1) {
//                        userNpasswd = credentials.substring(0, credentials.indexOf(":"));
//                    }

//                final String credentials = new String(Base64.decodeBase64(autheader.substring(index)), "UTF8");

//                final String token = credentials.substring(credentials.indexOf(":") + 1);
            }
        }
        return userNpasswd;
    }

    public JDBCatalogue getCatalogue() {
        if (catalogue == null) {
            String jndiName = "bean/JDBCatalog";
            javax.naming.Context ctx;
            try {
                ctx = new InitialContext();
                if (ctx == null) {
                    throw new Exception("JNDI could not create InitalContext ");
                }
                javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
                catalogue = (JDBCatalogue) envContext.lookup(jndiName);
            } catch (Exception ex) {
                Logger.getLogger(CatalogueHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return catalogue;
    }

    private void recordEvent(Connection connection, HttpServletRequest httpServletRequest, double elapsed) throws SQLException, UnsupportedEncodingException {
        getCatalogue().recordRequest(connection, httpServletRequest, elapsed);
        connection.commit();
    }

    private FileAccessPredictor getFileAccessPredictor() throws Exception {
        if (fap == null) {
            String jndiName = "bean/Predictor";
            javax.naming.Context ctx;
            try {
                ctx = new InitialContext();
                if (ctx == null) {
                    throw new Exception("JNDI could not create InitalContext ");
                }
                javax.naming.Context envContext = (javax.naming.Context) ctx.lookup("java:/comp/env");
                fap = (FileAccessPredictor) envContext.lookup(jndiName);
                fap.startGraphPopulation();
            } catch (Exception ex) {
                Logger.getLogger(CatalogueHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fap;
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            getFileAccessPredictor().stopGraphPopulation();
            if (recordertimer != null) {
                if (recorder != null) {
                    recorder.comitToDB();
                }
                this.recordertimer.cancel();
            }

        } catch (Exception ex) {
            Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void predict(Method method, String reqURL) throws MalformedURLException {
        URL url = new URL(reqURL);
        if (url.toString().endsWith("/")) {
            URL newURL = new URL(url.toString().substring(0, url.toString().length() - 1));
            url = newURL;
        }
        
        LobState currentState = new LobState(method, url.getPath());
        LobState nextState = null;
        try {

            nextState = getFileAccessPredictor().predictNextState(currentState);
            if (nextState != null) {
                log.log(Level.INFO, "nextFile: {0}", nextState.getID());
            }
        } catch (Exception ex) {
            Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (prevState != null && nextState != null) {
            if (currentState.getID().equals(prevState.getID())) {
                log.log(Level.INFO, "Hit. currentState: {0} nextState: {1} prevState: {2}",
                        new Object[]{currentState.getID(), nextState.getID(), prevState.getID()});
            } else {
                log.log(Level.INFO, "Miss. currentState: {0} nextState: {1} prevState: {2}",
                        new Object[]{currentState.getID(), nextState.getID(), prevState.getID()});
            }
        }
        prevState = nextState;
    }

    private void startRecorder() {
        if (recorder == null) {
            recorder = new RequestEventRecorder(queue, getCatalogue());
            TimerTask gcTask = new MyTask(recorder);

            recordertimer = new Timer(true);
            recordertimer.schedule(gcTask, 900, 900);
        }

    }

    private static class RequestEventRecorder implements Runnable {

        private final BlockingQueue queue;
        private final JDBCatalogue catalogue;
        private List<RequestWapper> req = new ArrayList<>();

        private RequestEventRecorder(BlockingQueue queue, JDBCatalogue catalogue) {
            this.queue = queue;
            this.catalogue = catalogue;
        }

        @Override
        public void run() {
            try {
                RequestWapper rw = (RequestWapper) queue.take();
//                log.log(Level.FINE, "RequestWapper: {0} {1} {2} {3} {4} {5} {6} {7} {8}", new Object[]{rw.contentType, rw.method, rw.remoteAddr, rw.requestURL, rw.userAgent, rw.userNpasswd, rw.contentLength, rw.date, rw.elapsed});

                req.add(rw);
                if (req.size() >= 50) {
                    try {
                        comitToDB();
                    } catch (UnsupportedEncodingException ex) {
                        Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void comitToDB() throws UnsupportedEncodingException {
            if (req != null && !req.isEmpty()) {
                try (Connection connection = catalogue.getConnection()) {
                    catalogue.recordRequests(connection, req);
                    connection.commit();
                    req.clear();
//                connection.close();
                } catch (SQLException ex) {
                    Logger.getLogger(MyFilter.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
