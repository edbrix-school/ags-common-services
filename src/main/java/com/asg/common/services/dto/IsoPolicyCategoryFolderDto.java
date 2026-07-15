package com.asg.common.services.dto;

import lombok.Data;

import java.util.List;

/**
 * One folder in the Home page Documents widget. The widget's tree is two levels — category (folder)
 * -> attachment (file): every file belonging to a document in this category is collected directly
 * under the folder, the documents themselves are not a level of their own.
 * <p>
 * Each file still carries this employee's own access and acknowledgement state.
 */
@Data
public class IsoPolicyCategoryFolderDto {

    /** Discriminator for the widget's tree: this node renders as a folder. */
    private final String type = "folder";

    /** The raw {@code CATEGORY} value the documents were grouped on — the folder's key. */
    private String categoryCode;

    /** The folder's display name. Currently the same as {@link #categoryCode}. */
    private String category;

    private int attachmentCount;

    /** Files in this folder the employee still owes an acknowledgement for — the folder's badge. */
    private int acknowledgementPendingCount;

    private List<IsoPolicyAttachmentDto> attachments;
}
