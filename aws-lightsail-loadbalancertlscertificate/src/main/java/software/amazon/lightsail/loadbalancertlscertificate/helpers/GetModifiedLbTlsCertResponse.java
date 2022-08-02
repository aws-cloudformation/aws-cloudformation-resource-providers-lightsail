package software.amazon.lightsail.loadbalancertlscertificate.helpers;

import software.amazon.awssdk.services.lightsail.model.GetLoadBalancerTlsCertificatesResponse;

/**
 * We are using the LoadBalancerTlsCertificate resource type to modify the HttpsRedirectionEnabled parameter of the LoadBalancer.
 * As this is a parameter of the LoadBalancer, this is not part of the response of GetLoadBalancerTlsCertificatesResponse. So,
 * we are modifying the response object to also include this parameter.
 */
public class GetModifiedLbTlsCertResponse {

    private GetLoadBalancerTlsCertificatesResponse LbTlsCertResponse;
    private boolean httpsRedirectionEnabled;

    public GetModifiedLbTlsCertResponse(GetLoadBalancerTlsCertificatesResponse lbTlsCertResponse, boolean httpsRedirectionEnabled) {
        LbTlsCertResponse = lbTlsCertResponse;
        this.httpsRedirectionEnabled = httpsRedirectionEnabled;
    }

    public GetModifiedLbTlsCertResponse() {
    }

    public GetLoadBalancerTlsCertificatesResponse getLbTlsCertResponse() {
        return LbTlsCertResponse;
    }

    public void setLbTlsCertResponse(GetLoadBalancerTlsCertificatesResponse lbTlsCertResponse) {
        LbTlsCertResponse = lbTlsCertResponse;
    }

    public boolean isHttpsRedirectionEnabled() {
        return httpsRedirectionEnabled;
    }

    public void setHttpsRedirectionEnabled(boolean httpsRedirectionEnabled) {
        this.httpsRedirectionEnabled = httpsRedirectionEnabled;
    }
}
