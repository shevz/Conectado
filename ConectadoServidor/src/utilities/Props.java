package utilities;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class Props {
	private InputStream inputStream;

	public Properties getProps() {
		Properties prop = new Properties();
		try {
			String propFileName = "servidor.properties";
			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
			if (inputStream != null) {
				prop.load(inputStream);
				Logging.getLogger().info("Properties file loaded");
			} else {
				Logging.getLogger().info("property file '" + propFileName + "' not found in the classpath");
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
		} catch (Exception e) {
			System.out.println("Exception: " + e);
			Logging.getLogger().error(e);
		}
		return prop;
	}
}