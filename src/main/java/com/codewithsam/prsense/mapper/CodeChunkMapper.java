package com.codewithsam.prsense.mapper;

import com.codewithsam.prsense.dto.response.CodeChunkDto;
import com.codewithsam.prsense.entity.CodeChunkEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CodeChunkMapper {

    CodeChunkDto toDto(CodeChunkEntity entity);

    List<CodeChunkDto> toDtoList(List<CodeChunkEntity> entities);
}
