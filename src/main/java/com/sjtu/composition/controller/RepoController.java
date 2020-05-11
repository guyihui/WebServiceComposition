package com.sjtu.composition.controller;

import com.sjtu.composition.serviceUtils.Service;
import com.sjtu.composition.serviceUtils.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RepoController {

    private ServiceRepository serviceRepository;

    @Autowired
    public RepoController(@Qualifier("serviceRepository") ServiceRepository serviceRepository) {
        Assert.notNull(serviceRepository, "service repository must not be null");
        this.serviceRepository = serviceRepository;
    }


    @GetMapping("/repo")
    public List<?> getServicesFromRepo(@RequestParam(value = "summary", required = false) String summary) {
        if (summary == null) {
            return serviceRepository.getServiceList();
        } else {
            return serviceRepository.getServiceStringList();
        }
    }

    @GetMapping("/repo/{id}")
    public Service getServiceById(@PathVariable("id") int id) {
        return serviceRepository.getServiceById(id);
    }

}
