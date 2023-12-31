package com.haw.task.controller.impl;

import com.haw.appuser.common.exceptions.UserNotFoundByUsernameException;
import com.haw.appuser.dataaccess.api.repo.UserRepository;
import com.haw.appuser.logic.api.UserService;
import com.haw.task.common.TaskDto;
import com.haw.task.common.TaskNotFoundException;
import com.haw.task.common.ForbiddenOperationException;
import com.haw.task.dataaccess.api.entity.Task;
import com.haw.task.dataaccess.api.repo.TaskRepository;
import com.haw.task.logic.impl.AudioUtils;
import com.haw.task.logic.impl.SpeechToTextService;
import com.haw.task.logic.impl.TaskServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    public
    TaskServiceImpl taskService;

    @Autowired
    UserRepository userRepository;
    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserService userService;

    @Autowired
    SpeechToTextService speechToTextService;


    @GetMapping
    public List<Task> getTasksForCurrentUser() throws UserNotFoundByUsernameException {
        return taskService.getAllTasks();
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long taskId) throws UserNotFoundByUsernameException {
        Task task = taskService.getTaskById(taskId);
        return ResponseEntity.ok(task);
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody TaskDto task) throws UserNotFoundByUsernameException {
        Task newTask = taskService.createTask(task);
        // Return the saved task and a 201 Created response
        return ResponseEntity.status(201).body(newTask);
    }

    @PostMapping("/transcriptions")
    public ResponseEntity<Map<String, String>> transcribeAudio(@RequestParam("audioFile") MultipartFile audioFile) {
        //transcription logic
        try {
            byte[] stereoAudioData = audioFile.getBytes();

            // Convert stereo to mono
            byte[] monoAudioData = AudioUtils.convertStereoToMono(stereoAudioData);

            String transcription = speechToTextService.transcribe(monoAudioData);

            // map to structure the response
            Map<String, String> response = new HashMap<>();
            response.put("transcription", transcription);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<Task> updateTask(@PathVariable Long taskId, @RequestBody TaskDto task)
            throws UserNotFoundByUsernameException, TaskNotFoundException, ForbiddenOperationException {

        Task newTask = taskService.updateTask(taskId, task);
        return ResponseEntity.status(200).body(newTask);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(@PathVariable Long taskId) throws TaskNotFoundException, UserNotFoundByUsernameException, ForbiddenOperationException {

        try {
            taskService.deleteTask(taskId);
        } catch (TaskNotFoundException | ForbiddenOperationException | UserNotFoundByUsernameException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok("Task with ID " + taskId + " has been deleted");
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    private ResponseEntity<String> handleUnauthorizedOperationException(ForbiddenOperationException ex) {
        int statusCode = ex.getStatusCode();
        String errorMessage = ex.getMessage();
        return ResponseEntity.status(statusCode).body(errorMessage);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    private ResponseEntity<String> handleTaskNotFoundException(TaskNotFoundException ex) {
        int statusCode = ex.getStatusCode();
        String errorMessage = ex.getMessage();
        return ResponseEntity.status(statusCode).body(errorMessage);
    }
}
