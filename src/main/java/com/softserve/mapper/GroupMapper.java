package com.softserve.mapper;

import com.softserve.dto.GroupDTO;
import com.softserve.dto.GroupDTOInRoomSchedule;
import com.softserve.dto.GroupForUpdateDTO;
import com.softserve.dto.GroupWithStudentsDTO;
import com.softserve.entity.Group;
import com.softserve.entity.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface GroupMapper {
    GroupDTO groupToGroupDTO(Group group);

    Group groupDTOToGroup(GroupDTO groupDTO);

    GroupForUpdateDTO groupToGroupForUpdateDTO(Group group);

    GroupWithStudentsDTO groupToGroupWithStudentsDTO(Group group);

    Group groupForUpdateDTOToGroup(GroupForUpdateDTO groupForUpdateDTO);

    List<GroupDTO> groupsToGroupDTOs(List<Group> groups);

    @Mapping(source = "id", target = "groupId")
    @Mapping(source = "title", target = "groupName")
    GroupDTOInRoomSchedule groupToGroupDTOInRoomSchedule(Group group);

    List<GroupDTOInRoomSchedule> toGroupDTOInRoomSchedule(List<Group> group);

}
