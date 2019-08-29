import com.hedera.services.exchange.ExchangeRateTool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;

public class Main {

	public void execute(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final String configFileName = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		ExchangeRateTool.main(configFileName);
	}
}
