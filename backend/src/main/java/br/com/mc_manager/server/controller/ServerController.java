package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.ProviderResponseDTO;
import br.com.mc_manager.server.dto.StatusResponseDTO;
import br.com.mc_manager.server.dto.server.CreateServerDTO;
import br.com.mc_manager.server.dto.server.ServerDetailsDTO;
import br.com.mc_manager.server.dto.server.UpdateServerDTO;
import br.com.mc_manager.server.model.ServerType;
import br.com.mc_manager.server.service.ServerManagementService;
import br.com.mc_manager.server.service.provider.ServerProviderRegistry;
import br.com.mc_manager.server.service.runtime.ServerRuntimeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/servers")
public class ServerController {

  private final ServerRuntimeService runtimeService;
  private final ServerManagementService managementService;
  private final ServerProviderRegistry registry;

  public ServerController(
          ServerRuntimeService runtimeService,
          ServerManagementService managementService,
          ServerProviderRegistry registry
  ) {
    this.runtimeService = runtimeService;
    this.managementService = managementService;
    this.registry = registry;
  }

  @GetMapping
  public ResponseEntity<List<ServerDetailsDTO>> listServers(Principal principal) {
    var servers = managementService.listServers(principal.getName());

    return ResponseEntity.ok().body(servers);
  }

  @GetMapping("/{serverId}")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<ServerDetailsDTO> getServer(@PathVariable Long serverId) {
    var server = managementService.getServer(serverId);

    return ResponseEntity.ok().body(server);
  }

  @PostMapping
  public ResponseEntity<ServerDetailsDTO> createServer(
          @Valid @RequestBody CreateServerDTO dto,
          Principal principal,
          UriComponentsBuilder uriBuilder
  ) {
    var server = managementService.create(dto, principal);

    var uri = uriBuilder.path("/api/servers/{id}").buildAndExpand(server.id()).toUri();

    return ResponseEntity.created(uri).body(server);
  }

  @PutMapping("/{serverId}")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<ServerDetailsDTO> update(
          @PathVariable Long serverId,
          @Valid @RequestBody UpdateServerDTO dto
  ) {
    var server = managementService.update(serverId, dto);
    runtimeService.restartWithUpdatedConfig(serverId);

    return ResponseEntity.ok().body(server);
  }

  @DeleteMapping("/{serverId}")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> delete(@PathVariable Long serverId) {
    managementService.delete(serverId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{serverId}/start")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> start(@PathVariable Long serverId) {
    runtimeService.start(serverId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{serverId}/stop")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> stop(@PathVariable Long serverId) {
    runtimeService.stop(serverId);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{serverId}/restart")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> restart(@PathVariable Long serverId) {
    runtimeService.restart(serverId);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{serverId}/status")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<StatusResponseDTO> getStatus(@PathVariable Long serverId) {
    var status = runtimeService.getStatus(serverId);
    var statusResponse = new StatusResponseDTO(status);

    return ResponseEntity.ok().body(statusResponse);
  }

  @GetMapping("/catalog")
  public ResponseEntity<List<ProviderResponseDTO>> listServerTypes() {
    var catalog = registry.getProviderCatalog();
    return ResponseEntity.ok(catalog);
  }

  @GetMapping("/versions")
  public ResponseEntity<List<String>> getAllVersions(@RequestParam ServerType type) {
    var allVersions = registry.get(type).getAllVersions();
    return ResponseEntity.ok(allVersions);
  }

}
