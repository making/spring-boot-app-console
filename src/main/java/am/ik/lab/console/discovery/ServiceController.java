package am.ik.lab.console.discovery;

import am.ik.lab.console.security.JwtService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
public class ServiceController {

    private final DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate;

    private final JwtService jwtService;

    public ServiceController(DiscoveryClient discoveryClient, RestTemplateBuilder builder, JwtService jwtService) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = builder
            .errorHandler(new ResponseErrorHandler() {

                @Override
                public boolean hasError(ClientHttpResponse response) throws IOException {
                    return false;
                }

                @Override
                public void handleError(ClientHttpResponse response) throws IOException {

                }
            })
            .build();
        this.jwtService = jwtService;
    }

    @GetMapping("services")
    public Object services() {
        final Map<String, List<ServiceInstance>> applications = this.discoveryClient.getServices().stream().collect(Collectors.toMap(Function.identity(), this.discoveryClient::getInstances));
        return applications;
    }

    @GetMapping("services/{serviceId}/{instanceId}/cloudfoundryapplication/**")
    public Object proxy(@PathVariable("serviceId") String serviceId, @PathVariable("instanceId") String instanceId, HttpServletRequest request, UriComponentsBuilder builder) {
        final RequestPath requestPath = RequestPath.parse(new ServletServerHttpRequest(request).getURI(), request.getContextPath());
        // / services / {serviceId} / {instanceId} / cloudfoundryapplication / **
        // ^____^_____^______^______^______^_______^________^___________________^
        // 0____1_____2______3______4______5_______6________7___________________8

        final String subPath = requestPath.subPath(7).value();
        final List<ServiceInstance> instances = this.discoveryClient.getInstances(serviceId);
        final Optional<ServiceInstance> serviceInstance = instances.stream().filter(i -> Objects.equals(i.getInstanceId(), instanceId)).findAny();
        return serviceInstance
            .map(instance -> {
                final String jwt = this.jwtService.generateToken(builder.replacePath("").build().toUriString(), serviceId).serialize();
                final URI uri = UriComponentsBuilder.fromUri(instance.getUri()).replacePath(subPath).build().toUri();
                final RequestEntity<Void> requestEntity = RequestEntity.get(uri)
                    .header(AUTHORIZATION, "Bearer " + jwt)
                    .build();
                return this.restTemplate.exchange(requestEntity, String.class);
            })
            .orElseGet((() -> ResponseEntity.notFound().build()));
    }
}
