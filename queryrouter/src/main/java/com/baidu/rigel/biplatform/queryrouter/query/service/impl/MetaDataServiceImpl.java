/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.baidu.rigel.biplatform.queryrouter.query.service.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Service;

import com.baidu.rigel.biplatform.ac.exception.MiniCubeQueryException;
import com.baidu.rigel.biplatform.ac.minicube.MiniCube;
import com.baidu.rigel.biplatform.ac.minicube.MiniCubeMember;
import com.baidu.rigel.biplatform.ac.model.Cube;
import com.baidu.rigel.biplatform.ac.model.Dimension;
import com.baidu.rigel.biplatform.ac.model.Level;
import com.baidu.rigel.biplatform.ac.model.LevelType;
import com.baidu.rigel.biplatform.ac.query.data.DataSourceInfo;
import com.baidu.rigel.biplatform.ac.util.DeepcopyUtils;
import com.baidu.rigel.biplatform.ac.util.MetaNameUtil;
import com.baidu.rigel.biplatform.queryrouter.query.exception.MetaException;
import com.baidu.rigel.biplatform.queryrouter.query.service.DimensionMemberService;
import com.baidu.rigel.biplatform.queryrouter.query.service.MetaDataService;
import com.baidu.rigel.biplatform.queryrouter.queryplugin.jdbc.connection.DataSourcePoolService;
import com.google.common.collect.Lists;

/**
 * 元数据实际查询操作实现
 * 
 * @author xiaoming.chen
 *
 */
@Service
public class MetaDataServiceImpl implements MetaDataService, BeanFactoryAware {

    /**
     * dataSourcePoolService
     */
    @Resource
    private DataSourcePoolService dataSourcePoolService;

    private static Map<String, Cube> storeManager = new ConcurrentHashMap<String, Cube>();

    /**
     * dimensionMemberServiceMap 获取维值的所有实现
     */
    private Map<String, DimensionMemberService> dimensionMemberServiceMap;

    @Override
    public Cube getCube(String cubeId) throws MiniCubeQueryException {
        if (StringUtils.isBlank(cubeId)) {
            throw new IllegalArgumentException("can not get cube by empty cube id.");
        }
        Cube result = storeManager.get(cubeId);
        if (result == null) {
            throw new MiniCubeQueryException("can not get cube by cackeKey:" + cubeId);
        }
        return result;
    }

    @Override
    public void cacheCube(Cube cube) {
        if (cube == null) {
            throw new IllegalArgumentException("cube is null");
        }
        storeManager.put(cube.getId(), cube);
    }

    @Override
    public List<MiniCubeMember> getMembers(String dataSourceInfoKey, String cubeId, String dimensionName,
            String levelName, Map<String, String> params) throws MiniCubeQueryException, MetaException {
        DataSourceInfo dataSourceInfo = dataSourcePoolService.getDataSourceInfo(dataSourceInfoKey);
        if (dataSourceInfo == null || !dataSourceInfo.validate()) {
            throw new MiniCubeQueryException("dataSourceInfo is null or invalidate :" + dataSourceInfo);
        }
        Cube cube = getCube(cubeId);
        if (cube == null) {
            throw new MiniCubeQueryException("can not get cube by cubeId:" + cubeId);
        }
        return getMembers(dataSourceInfo, cube, dimensionName, levelName, params);

    }

    @Override
    public List<MiniCubeMember> getMembers(DataSourceInfo dataSource, Cube cube, String dimensionName,
            String levelName, Map<String, String> params) throws MiniCubeQueryException, MetaException {
        MetaDataService.checkCube(cube);
        MetaDataService.checkDataSourceInfo(dataSource);
        Dimension dimension = cube.getDimensions().get(dimensionName);
        if (dimension == null) {
            throw new MiniCubeQueryException("cube:" + cube + " not contain dimension:" + dimensionName);
        }
        Level level = dimension.getLevels().get(levelName);
        if (level == null) {
            throw new MiniCubeQueryException("can not get level by name:" + levelName + " in dimension:" + dimension);
        }
        return DimensionMemberService.getDimensionMemberServiceByLevelType(level.getType()).getMembers(cube, level,
                dataSource, null, params);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        ListableBeanFactory listBeanFactory = (ListableBeanFactory) beanFactory;
        dimensionMemberServiceMap = listBeanFactory.getBeansOfType(DimensionMemberService.class);
    }

    @Override
    public List<MiniCubeMember> getChildren(String dataSourceInfoKey, String cubeId, String uniqueName,
            Map<String, String> params) throws MiniCubeQueryException, MetaException {

        DataSourceInfo dataSourceInfo = dataSourcePoolService.getDataSourceInfo(dataSourceInfoKey);
        Cube cube = getCube(cubeId);
        return getChildren(dataSourceInfo, cube, uniqueName, params);

    }

    @Override
    public List<MiniCubeMember> getChildren(DataSourceInfo dataSource, Cube cube, String uniqueName,
            Map<String, String> params) throws MiniCubeQueryException, MetaException {

        DimensionMemberService memberService = dimensionMemberServiceMap.get(DimensionMemberService.SQL_MEMBER_SERVICE);
        MiniCubeMember member = memberService.lookUp(dataSource, cube, uniqueName, params);
        return getChildren(dataSource, cube, member, params);
    }

    @Override
    public List<MiniCubeMember> getChildren(String dataSourceInfoKey, String cubeId, MiniCubeMember member,
            Map<String, String> params) throws MiniCubeQueryException, MetaException {
        DataSourceInfo dataSourceInfo = dataSourcePoolService.getDataSourceInfo(dataSourceInfoKey);
        Cube cube = getCube(cubeId);
        return getChildren(dataSourceInfo, cube, member, params);
    }

    @Override
    public List<MiniCubeMember> getChildren(DataSourceInfo dataSource, Cube cube, MiniCubeMember member,
            Map<String, String> params) throws MiniCubeQueryException, MetaException {
        MetaDataService.checkCube(cube);
        MetaDataService.checkDataSourceInfo(dataSource);
        Dimension dimension = member.getLevel().getDimension();
        Level level = member.getLevel();
        if (!member.isAll() && !level.getType().equals(LevelType.CALL_BACK)) {
            List<Level> levels = Lists.newArrayList(dimension.getLevels().values());
            int levelIndex = -1;
            for (int i = 0; i < levels.size(); i++) {
                if (levels.get(i).getName().equals(member.getLevel().getName())) {
                    levelIndex = i;
                    break;
                }
            }
            if (level.getType().equals(LevelType.PARENT_CHILD)) {
                level = levels.get(levelIndex);
            } else if (levelIndex == -1 || (levelIndex + 1) >= levels.size()) {
                return Lists.newArrayList();
                // LOG.error("can not get level :" + member.getLevel().getName() + " in dimension:" + dimension);
                // throw new MetaException("can not get level :" + member.getLevel().getName() + " in dimension:" +
                // dimension);
            } else {
                // 取当前层级的下一层级
                level = levels.get(levelIndex + 1);
            }
        }

        return DimensionMemberService.getDimensionMemberServiceByLevelType(level.getType()).getMembers(cube, level,
                dataSource, member, params);
    }

    @Override
    public MiniCubeMember lookUp(String dataSourceKey, String cubeId, String uniqueName, Map<String, String> params)
            throws MiniCubeQueryException, MetaException {
        DataSourceInfo dataSourceInfo = dataSourcePoolService.getDataSourceInfo(dataSourceKey);
        MetaDataService.checkDataSourceInfo(dataSourceInfo);
        Cube cube = getCube(cubeId);
        MetaDataService.checkCube(cube);
        return lookUp(dataSourceInfo, cube, uniqueName, params);
    }

    @Override
    public MiniCubeMember lookUp(DataSourceInfo dataSource, Cube cube, String uniqueName, Map<String, String> params)
            throws MiniCubeQueryException, MetaException {
        MetaDataService.checkCube(cube);
        MetaDataService.checkDataSourceInfo(dataSource);
        DimensionMemberService memberService = dimensionMemberServiceMap.get(DimensionMemberService.SQL_MEMBER_SERVICE);
        return memberService.lookUp(dataSource, cube, uniqueName, params);
    }

    @Override
    public List<MiniCubeMember> lookUp(DataSourceInfo dataSourceInfo,
            Cube cube, List<String> uniqueNameList, Map<String, String> params)
            throws Exception {
        MetaDataService.checkCube(cube);
        MetaDataService.checkDataSourceInfo(dataSourceInfo);
        DimensionMemberService memberService = dimensionMemberServiceMap.get(DimensionMemberService.SQL_MEMBER_SERVICE);
        Dimension dim = null;
        Level level = null;
        String dimName = null;
        String[] dimNameArray = null;
        Iterator<Level> it = null;
        Iterator<String> unqNameIt = uniqueNameList.iterator ();
        String uniqueName = null;
        List<String> oldNames = DeepcopyUtils.deepCopy (uniqueNameList);
        List<String> queryUniqueName = Lists.newArrayList ();
        while (unqNameIt.hasNext ()) {
            uniqueName = unqNameIt.next ();
            dimName = MetaNameUtil.getDimNameFromUniqueName (uniqueName);
            dim = cube.getDimensions ().get (dimName);
            if (dim.getLevels ().size () > 1) {
                dimNameArray = MetaNameUtil.parseUnique2NameArray (uniqueName);
                it = dim.getLevels ().values ().iterator ();
                for (int i = 0; i < dimNameArray.length - 2; ++i) {
                    it.next ();
                }
                level = it.next ();
                if (level.getDimTable ().equals (((MiniCube) cube).getSource())) {
                    unqNameIt.remove ();
                    queryUniqueName.add(uniqueName.substring (0, uniqueName.lastIndexOf (".")));
                }
            }
        }
        Iterator<MiniCubeMember> memberIt = null;
        List<MiniCubeMember> members = Lists.newArrayList ();
        if (!queryUniqueName.isEmpty ()) {
            List<MiniCubeMember> parents = memberService.lookUpByNames(dataSourceInfo, cube, queryUniqueName, params);
            for (MiniCubeMember parent : parents) {
                members.addAll (memberService.getMembers (cube, level, dataSourceInfo, parent, params));
            }
            memberIt = members.iterator ();
            while (memberIt.hasNext ()) {
                if (!oldNames.contains (memberIt.next ().getUniqueName ())) {
                    memberIt.remove ();
                }
            }
        }
        if (!uniqueNameList.isEmpty ()) {
            members.addAll (memberService.lookUpByNames(dataSourceInfo, cube, uniqueNameList, params));
        }
//        List<MiniCubeMember> members = memberService.lookUpByNames(dataSourceInfo, cube, uniqueNameList, params);
        
        return members;
    }
}
