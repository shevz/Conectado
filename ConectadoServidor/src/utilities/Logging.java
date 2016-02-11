package utilities;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Logging {
	private static final Logger logger=LogManager.getLogger(Logging.class);
	
	public static Logger getLogger() {
		return logger;
	}

	Logging()
	{
		PropertyConfigurator.configure("log4j.properties");
	}

}
