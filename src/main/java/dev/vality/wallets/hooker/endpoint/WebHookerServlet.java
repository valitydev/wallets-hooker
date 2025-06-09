package dev.vality.wallets.hooker.endpoint;

import dev.vality.fistful.webhooker.WebhookManagerSrv;
import dev.vality.woody.api.event.CompositeServiceEventListener;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import dev.vality.woody.thrift.impl.http.event.HttpServiceEventLogListener;
import dev.vality.woody.thrift.impl.http.event.ServiceEventLogListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.IOException;

@Slf4j
@WebServlet("/hook")
public class WebHookerServlet extends GenericServlet {

    private Servlet thriftServlet;

    @Autowired
    private WebhookManagerSrv.Iface requestHandler;

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("Hooker servlet init.");
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .withEventListener(
                        new CompositeServiceEventListener(
                                new ServiceEventLogListener(),
                                new HttpServiceEventLogListener()))
                .build(WebhookManagerSrv.Iface.class, requestHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        log.info("Start new request to servlet.");
        thriftServlet.service(req, res);
    }
}
