package com.recruiting.platform.pipeline.bulk.app;

import com.recruiting.platform.contracts.BulkActionType;
import com.recruiting.platform.contracts.BulkResultCode;
import com.recruiting.platform.pipeline.BulkItemResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Tag rules shared by the pipeline handlers: at most 20 tags, no duplicates, NO_CHANGE when nothing differs. */
final class TagOps {

    static final int MAX_TAGS = 20;

    private TagOps() {
    }

    /**
     * Adds or removes the tag.
     *
     * @param current the tags of the record
     * @param action  ADD_TAG or REMOVE_TAG
     * @param tag     the tag, already trimmed and lower-case
     * @param save    receives the new list when it changed
     * @return the result for the record
     */
    static BulkItemResult apply(List<String> current, BulkActionType action, String tag, Consumer<List<String>> save) {
        List<String> tags = new ArrayList<>(current == null ? List.of() : current);
        if (action == BulkActionType.ADD_TAG) {
            if (tags.contains(tag)) {
                return BulkItemResult.skipped(BulkResultCode.NO_CHANGE);
            }
            if (tags.size() >= MAX_TAGS) {
                return BulkItemResult.failed(BulkResultCode.TOO_MANY_TAGS);
            }
            tags.add(tag);
        } else if (!tags.remove(tag)) {
            return BulkItemResult.skipped(BulkResultCode.NO_CHANGE);
        }
        save.accept(tags);
        return BulkItemResult.ok();
    }
}
