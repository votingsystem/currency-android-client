package org.currency;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface AppState {
    /**
     * @return true to keep processing, false to read more data.
     */
    boolean process(android.app.Activity context);

}