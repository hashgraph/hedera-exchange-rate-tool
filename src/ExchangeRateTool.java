import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.*;

public class ExchangeRateTool {

    // logger object to write logs into
    private static final Logger log = LogManager.getLogger(ExchangeRateTool.class.getName());

    // member variables
    private String m_privateKey;
    private List<String> m_exchangeAPIList;
    private String m_mainNetAPI;
    private String m_pricingDBAPI;
    private Double m_maxDelta;
    private Double m_prevMedian;
    private Double m_currMedian;
    private String m_hederaFileIdentifier;
    private Double m_frequency;

    public static void main(String args[]){
        // TODO : read the config file and save the parameters.

        // using the frequency read from the config file, spawn a thread that does the functions.
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate( new ERTproc(m_privateKey, m_exchangeAPIList, m_mainNetAPI, m_pricingDBAPI,
                m_maxDelta, m_prevMedian, m_currMedian, m_hederaFileIdentifier),
                0, m_frequency, Timer.Seconds);

        // we wait a while for the thread to finish executing and fetch the details the ERTproc writes to the
        // database and update prev and curr medians so that we can send them to the new thread.


    }

    public String getM_privateKey() {
        return m_privateKey;
    }

    public void setM_privateKey(String m_privateKey) {
        this.m_privateKey = m_privateKey;
    }

    public List<String> getM_exchangeAPIList() {
        return m_exchangeAPIList;
    }

    public void setM_exchangeAPIList(List<String> m_exchangeAPIList) {
        this.m_exchangeAPIList = m_exchangeAPIList;
    }

    public String getM_mainNetAPI() {
        return m_mainNetAPI;
    }

    public void setM_mainNetAPI(String m_mainNetAPI) {
        this.m_mainNetAPI = m_mainNetAPI;
    }

    public String getM_pricingDBAPI() {
        return m_pricingDBAPI;
    }

    public void setM_pricingDBAPI(String m_pricingDBAPI) {
        this.m_pricingDBAPI = m_pricingDBAPI;
    }

    public Double getM_maxDelta() {
        return m_maxDelta;
    }

    public void setM_maxDelta(Double m_maxDelta) {
        this.m_maxDelta = m_maxDelta;
    }

    public Double getM_prevMedian() {
        return m_prevMedian;
    }

    public void setM_prevMedian(Double m_prevMedian) {
        this.m_prevMedian = m_prevMedian;
    }

    public Double getM_currMedian() {
        return m_currMedian;
    }

    public void setM_currMedian(Double m_currMedian) {
        this.m_currMedian = m_currMedian;
    }

    public String getM_hederaFileIdentifier() {
        return m_hederaFileIdentifier;
    }

    public void setM_hederaFileIdentifier(String m_hederaFileIdentifier) {
        this.m_hederaFileIdentifier = m_hederaFileIdentifier;
    }

    public Double getM_frequency() {
        return m_frequency;
    }

    public void setM_frequency(Double m_frequency) {
        this.m_frequency = m_frequency;
    }
}
