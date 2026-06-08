package com.codewithsam.prsense.mcp.service.impl;

import com.codewithsam.prsense.mcp.dto.AnalyzeImpactRequest;
import com.codewithsam.prsense.mcp.dto.GetClassHierarchyRequest;
import com.codewithsam.prsense.mcp.dto.GetRelatedCodeRequest;
import com.codewithsam.prsense.mcp.exception.KnowledgeGraphException;
import com.codewithsam.prsense.mcp.graph.*;
import com.codewithsam.prsense.mcp.response.ClassHierarchyResult;
import com.codewithsam.prsense.mcp.response.ImpactAnalysisResult;
import com.codewithsam.prsense.mcp.response.RelatedCodeResult;
import com.codewithsam.prsense.mcp.service.KnowledgeGraphService;
import com.codewithsam.prsense.repository.CodeChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final CodeChunkRepository codeChunkRepository;
    private final RepositoryGraphBuilder graphBuilder;

    // Per-repository graph cache — invalidated on re-indexing
    private final Map<String, RepositoryGraph> graphCache = new ConcurrentHashMap<>();

    @Override
    public RepositoryGraph getOrBuildGraph(String repository) {
        return graphCache.computeIfAbsent(repository, repo -> {
            log.info("Building knowledge graph for repository '{}'", repo);
            return graphBuilder.build(repo, codeChunkRepository.findByRepository(repo));
        });
    }

    @Override
    public void invalidateGraph(String repository) {
        graphCache.remove(repository);
        log.info("Knowledge graph invalidated for repository '{}'", repository);
    }

    @Override
    public ClassHierarchyResult getClassHierarchy(GetClassHierarchyRequest request) {
        log.info("Class hierarchy lookup — repository: '{}', class: '{}'",
                request.getRepository(), request.getClassName());

        RepositoryGraph graph = getOrBuildGraph(request.getRepository());
        String className = request.getClassName();

        List<String> parents     = new ArrayList<>(graph.outgoing(className, EdgeType.EXTENDS));
        List<String> interfaces  = new ArrayList<>(graph.outgoing(className, EdgeType.IMPLEMENTS));
        List<String> children    = new ArrayList<>(graph.incoming(className, EdgeType.EXTENDS));
        List<String> childIfaces = new ArrayList<>(graph.incoming(className, EdgeType.IMPLEMENTS));

        log.info("Class hierarchy lookup completed — class: '{}', parents: {}, interfaces: {}, children: {}",
                className, parents.size(), interfaces.size(), children.size());

        return ClassHierarchyResult.builder()
                .className(className)
                .parentClasses(parents)
                .implementedInterfaces(interfaces)
                .childClasses(children)
                .childInterfaces(childIfaces)
                .build();
    }

    @Override
    public RelatedCodeResult getRelatedCode(GetRelatedCodeRequest request) {
        log.info("Related code retrieval — repository: '{}', class: '{}'",
                request.getRepository(), request.getClassName());

        RepositoryGraph graph = getOrBuildGraph(request.getRepository());
        String className = request.getClassName();

        // Classes this class DEPENDS_ON
        Set<String> deps = graph.outgoing(className, EdgeType.DEPENDS_ON);
        // Classes that DEPEND_ON this class
        Set<String> dependents = graph.incoming(className, EdgeType.DEPENDS_ON);

        Set<String> allRelated = new HashSet<>();
        allRelated.addAll(deps);
        allRelated.addAll(dependents);

        List<RelatedCodeResult.RelatedComponent> services = new ArrayList<>();
        List<RelatedCodeResult.RelatedComponent> repositories = new ArrayList<>();
        List<RelatedCodeResult.RelatedComponent> controllers = new ArrayList<>();
        List<RelatedCodeResult.RelatedComponent> dtos = new ArrayList<>();
        List<RelatedCodeResult.RelatedComponent> entities = new ArrayList<>();

        for (String relatedId : allRelated) {
            graph.getNode(relatedId).ifPresent(node -> {
                RelatedCodeResult.RelatedComponent comp = RelatedCodeResult.RelatedComponent.builder()
                        .className(node.getSimpleName())
                        .filePath(node.getFilePath())
                        .symbolType(node.getType().name())
                        .build();

                String lower = relatedId.toLowerCase();
                if (lower.contains("service"))         services.add(comp);
                else if (lower.contains("repository") || lower.contains("repo")) repositories.add(comp);
                else if (lower.contains("controller")) controllers.add(comp);
                else if (lower.contains("dto") || lower.contains("request") || lower.contains("response")) dtos.add(comp);
                else if (lower.contains("entity") || lower.contains("model")) entities.add(comp);
                else                                   services.add(comp); // default bucket
            });
        }

        log.info("Related code retrieval completed — class: '{}', related: {}", className, allRelated.size());

        return RelatedCodeResult.builder()
                .className(className)
                .services(services).repositories(repositories)
                .controllers(controllers).dtos(dtos).entities(entities)
                .build();
    }

    @Override
    public ImpactAnalysisResult analyzeImpact(AnalyzeImpactRequest request) {
        log.info("Impact analysis — repository: '{}', class: '{}'",
                request.getRepository(), request.getClassName());

        RepositoryGraph graph = getOrBuildGraph(request.getRepository());
        String className = request.getClassName();

        // Everything that DEPENDS_ON this class
        Set<String> directDependents = graph.incoming(className, EdgeType.DEPENDS_ON);
        // Also callers at method level
        Set<String> callers = graph.allNodes().stream()
                .filter(n -> !graph.outgoing(n.getId(), EdgeType.CALLS).isEmpty())
                .filter(n -> graph.outgoing(n.getId(), EdgeType.CALLS).stream()
                        .anyMatch(t -> t.startsWith(className)))
                .map(RepositoryNode::getSimpleName)
                .collect(Collectors.toSet());

        Set<String> allAffected = new HashSet<>();
        allAffected.addAll(directDependents);
        allAffected.addAll(callers);

        List<String> controllers = new ArrayList<>();
        List<String> services    = new ArrayList<>();
        List<String> repos       = new ArrayList<>();
        List<String> scheduled   = new ArrayList<>();
        List<String> listeners   = new ArrayList<>();

        for (String affected : allAffected) {
            String lower = affected.toLowerCase();
            if (lower.contains("controller"))                       controllers.add(affected);
            else if (lower.contains("repository") || lower.contains("repo")) repos.add(affected);
            else if (lower.contains("scheduled") || lower.contains("job"))   scheduled.add(affected);
            else if (lower.contains("listener") || lower.contains("handler")) listeners.add(affected);
            else                                                     services.add(affected);
        }

        log.info("Impact analysis completed — class: '{}', total affected: {}",
                className, allAffected.size());

        return ImpactAnalysisResult.builder()
                .className(className)
                .affectedControllers(controllers).affectedServices(services)
                .affectedRepositories(repos).affectedScheduledJobs(scheduled)
                .affectedEventListeners(listeners)
                .totalAffectedComponents(allAffected.size())
                .build();
    }
}
