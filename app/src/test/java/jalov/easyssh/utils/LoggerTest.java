package jalov.easyssh.utils;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by jalov on 2018-02-26.
 */
public class LoggerTest {
    private final String FIRST_LOG = "First log";
    private final String SECOND_LOG = "Second log";
    private final String RUNNING_SERVER_MESSAGE = "Server listening on :: port 22";
    private final String NOT_RUNNING_SERVER_MESSAGE = "Server error";
    private Logger logger;

    @Before
    public void setUp() throws Exception {
        logger = new Logger();
    }

    @Test
    public void shouldAddMessageToLogList() throws Exception {
        assertEquals(0, logger.getLogs().size());
//        BaseAdapter adapter = mock(BaseAdapter.class);

//        logger.setAdapter(adapter);
        logger.log(FIRST_LOG);

        assertEquals(1, logger.getLogs().size());
//        verify(adapter, times(1)).notifyDataSetChanged();
    }

    @Test
    public void shouldStoreLogsInInvertedOrder() throws Exception {
        logger.log(FIRST_LOG);
        logger.log(SECOND_LOG);

        List<String> logs = logger.getLogs();

        assertEquals(SECOND_LOG, logs.get(0));
        assertEquals(FIRST_LOG, logs.get(1));
    }

    @Test
    public void registerSshServerInputStreamShouldReturnTrueIfServerStartedCorrectly() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(RUNNING_SERVER_MESSAGE.getBytes());
        assertEquals(true, logger.registerSshServerInputStream(inputStream));

        List<String> logs = logger.getLogs();
        assertEquals(1, logs.size());
    }

    @Test
    public void registerSshServerInputStreamShouldReturnFalseIfServerStartFailed() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(NOT_RUNNING_SERVER_MESSAGE.getBytes());
        assertEquals(false, logger.registerSshServerInputStream(inputStream));

        List<String> logs = logger.getLogs();
        assertEquals(1, logs.size());
    }
}