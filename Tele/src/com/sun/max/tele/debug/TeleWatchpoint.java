package com.sun.max.tele.debug;

import java.util.HashMap;
import java.util.Map;

import com.sun.max.memory.MemoryRegion;
import com.sun.max.tele.TeleViewModel;

/**
 * @author Bernd Mathiske
 */
public class TeleWatchpoint {

    private final TeleProcess _teleProcess;

    public TeleProcess teleProcess() {
        return _teleProcess;
    }

    private final MemoryRegion _memoryRegion;

    public MemoryRegion memoryRegion() {
        return _memoryRegion;
    }

    public TeleWatchpoint(TeleProcess teleProcess, MemoryRegion memoryRegion) {
        _teleProcess = teleProcess;
        _memoryRegion = memoryRegion;
    }

    public static class Factory extends TeleViewModel {

        private final TeleProcess _teleProcess;

        public Factory(TeleProcess teleProcess) {
            _teleProcess = teleProcess;
        }

        private final Map<MemoryRegion, TeleWatchpoint> _watchpoints = new HashMap<MemoryRegion, TeleWatchpoint>();

        /**
         * Creates a watchpoint for a given memory region.
         */
        public synchronized TeleWatchpoint createWatchpoint(MemoryRegion memoryRegion) {
            final TeleWatchpoint watchpoint = new TeleWatchpoint(_teleProcess, memoryRegion);
            _watchpoints.put(memoryRegion, watchpoint);
            refreshView(_teleProcess.epoch());
            return watchpoint;
        }

        /**
         * Creates a watchpoint that covers the given memoryRegion.
         * TODO: merge with existing watchpoints.
         */
        public synchronized TeleWatchpoint makeWatchpoint(MemoryRegion memoryRegion) {
            final TeleWatchpoint watchpoint =  createWatchpoint(memoryRegion);
            if (_teleProcess.activateWatchpoint(watchpoint.memoryRegion())) {
                return watchpoint;
            }
            return null;
        }
    }

}
