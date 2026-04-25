package com.yaobezyana.inbox.mapper;

import com.yaobezyana.inbox.dto.InboxNoteResponse;
import com.yaobezyana.inbox.entity.InboxNote;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InboxMapper {

    InboxNoteResponse toResponse(InboxNote note);
}
