package com.victor.filestorageapi.mapper;

import com.victor.filestorageapi.models.dtos.userfolder.UserFolderResponseDto;
import com.victor.filestorageapi.models.entities.UserFolder;
import org.springframework.stereotype.Service;

@Service
public class UserFolderMapper {
    public UserFolderResponseDto mapUserFolderToResponse(UserFolder userFolder){
        return new UserFolderResponseDto(
                userFolder.getName(),
                userFolder.getFolderPath(),
                userFolder.getIsRoot(),
                userFolder.getUser().getUsername()
        );
    }
}
