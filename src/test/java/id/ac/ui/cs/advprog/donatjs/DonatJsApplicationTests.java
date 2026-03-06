package id.ac.ui.cs.advprog.donatjs;

import id.ac.ui.cs.advprog.donatjs.controller.LandingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DonatJsApplicationTests {

	@Autowired
	private LandingController landingController;

	@Test
	void contextLoads() {
		assertThat(landingController).isNotNull();
	}
}