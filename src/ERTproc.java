import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/*
This class implements the methods that we perform periodically to generate Exchange rate
 */
public class ERTproc implements Runnable{
    // logger object to write logs into
    private static final Logger log = LogManager.getLogger(ERTproc.class.getName());

    private String m_privateKey;
    private List<String> m_exchangeAPIList;
    private String m_mainNetAPI;
    private String m_pricingDBAPI;
    private Double m_maxDelta;
    private Double m_prevMedian;
    private Double m_currMedian;
    private String m_hederaFileIdentifier;

    public ERTproc(String m_privateKey, List<String> m_exchangeAPIList, String m_mainNetAPI, String m_pricingDBAPI, Double m_maxDelta, Double m_prevMedian, Double m_currMedian, String m_hederaFileIdentifier) {
        this.m_privateKey = m_privateKey;
        this.m_exchangeAPIList = m_exchangeAPIList;
        this.m_mainNetAPI = m_mainNetAPI;
        this.m_pricingDBAPI = m_pricingDBAPI;
        this.m_maxDelta = m_maxDelta;
        this.m_prevMedian = m_prevMedian;
        this.m_currMedian = m_currMedian;
        this.m_hederaFileIdentifier = m_hederaFileIdentifier;
    }

    // now that we have all the data/APIs required, add methods to perform the functions
    @override
    public void run(){
        // we call the methods in the order of execution logic
    }
}
